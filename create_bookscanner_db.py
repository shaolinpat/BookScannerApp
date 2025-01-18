import mariadb
import sys
import os
from datetime import datetime
import argparse

# Database connection parameters
DB_CONFIG = {
    "user": "root",
    "password": "cng63hfc",
    "host": "localhost",
    "port": 3306
}

DATABASE_NAME = "BookDatabase"
BACKUP_FOLDER = "./data_backups"
BACKUP_FILE = f"{BACKUP_FOLDER}/bookdatabase_load.txt"

# SQL commands for database and tables
SQL_COMMANDS = [
    f"DROP DATABASE IF EXISTS {DATABASE_NAME};",
    f"CREATE DATABASE {DATABASE_NAME} CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci';",
    f"USE {DATABASE_NAME};",
    
    # Locations table
    """
    CREATE TABLE Locations (
        LocationID INT AUTO_INCREMENT COMMENT 'Location Identifier',
        LocationName VARCHAR(50) NOT NULL COMMENT 'Location Name (e.g., Office, Study, Box X)',
        PRIMARY KEY (LocationID),
        UNIQUE (LocationName)
    ) AUTO_INCREMENT=1;
    """,

    # Authors table
    """
    CREATE TABLE Authors (
        AuthorID INT AUTO_INCREMENT COMMENT 'Author Identifier',
        Prefix VARCHAR(10) NULL COMMENT 'Author Prefix (e.g., Dr.)',
        FirstName VARCHAR(50) NOT NULL COMMENT 'Author First Name',
        MiddleInitial VARCHAR(5) NULL COMMENT 'Author Middle Initial',
        LastName VARCHAR(50) NOT NULL COMMENT 'Author Last Name',
        Suffix VARCHAR(10) NULL COMMENT 'Author Suffix (e.g., Jr.)',
        PRIMARY KEY (AuthorID)
    ) AUTO_INCREMENT=1;
    """,
    
    # Books table
    """
    CREATE TABLE Books (
        BookID INT AUTO_INCREMENT COMMENT 'Book Identifier',
        ISBN VARCHAR(17) NULL COMMENT "ISBN 10 or ISBN 13 without dashes",
        Title VARCHAR(255) NOT NULL COMMENT 'Book Title',
        Publisher VARCHAR(100) NOT NULL COMMENT 'Book Publisher',
        Edition VARCHAR(25) NULL COMMENT 'Edition',
        PublicationDate DATE NULL COMMENT 'Date of Publication',
        LocationID INT NOT NULL COMMENT 'Location Identifier',
        IsRead BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Read Status',
        ReadDate DATE NULL COMMENT 'Date the Book was Read',
        DateEntered TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Date Entered into Database',
        PRIMARY KEY (BookID),
        CONSTRAINT FK_Location FOREIGN KEY (LocationID)
            REFERENCES Locations(LocationID)
            ON DELETE RESTRICT
            ON UPDATE RESTRICT
    ) AUTO_INCREMENT=1;
    """,
    
    # Books_Authors table
    """
    CREATE TABLE Books_Authors (
        BookID INT NOT NULL COMMENT 'Book Identifier',
        AuthorID INT NOT NULL COMMENT 'Author Identifier',
        PRIMARY KEY (BookID, AuthorID),
        CONSTRAINT FK_BookID FOREIGN KEY (BookID) REFERENCES Books(BookID) ON DELETE CASCADE ON UPDATE CASCADE,
        CONSTRAINT FK_AuthorID FOREIGN KEY (AuthorID) REFERENCES Authors(AuthorID) ON DELETE CASCADE ON UPDATE CASCADE
    );
    """
]

def ensure_backup_folder():
    """Ensure the backup folder exists."""
    if not os.path.exists(BACKUP_FOLDER):
        os.makedirs(BACKUP_FOLDER)

def export_data_to_file(connection, filename):
    """Export database contents to a file in the correct order."""
    cursor = connection.cursor()
    export_order = ["Locations", "Authors", "Books", "Books_Authors"]  # Ordered by dependency

    try:
        # Select the database
        cursor.execute(f"USE {DATABASE_NAME};")

        # Open the file for writing
        with open(filename, "w") as f:
            for table in export_order:
                # Write a comment with the table name
                f.write(f"-- Data for table {table}\n")
                
                # Fetch all rows from the table
                cursor.execute(f"SELECT * FROM {table};")
                rows = cursor.fetchall()
                
                # Skip tables without data
                if not rows:
                    f.write(f"-- No data in table {table}\n")
                    continue
                
                # Get column names dynamically
                cursor.execute(f"DESCRIBE {table};")
                columns = [col[0] for col in cursor.fetchall()]
                columns_str = ', '.join(columns)
                
                # Write insert statements for each row
                for row in rows:
                    values = ', '.join(
                        f"'{str(value)}'" if value is not None else 'NULL' for value in row
                    )
                    f.write(f"INSERT INTO {table} ({columns_str}) VALUES ({values});\n")
                    
        print(f"Data exported to {filename}")
    
    except Exception as e:
        print(f"Error exporting data: {e}")

def reload_data_from_file(connection, filename):
    """Reload database contents from a file."""
    with open(filename, "r") as f:
        cursor = connection.cursor()
        for command in f:
            if command.strip():
                cursor.execute(command)
        connection.commit()
    print(f"Data reloaded from {filename}")

def create_database(empty=False, reload_file=None):
    try:
        ensure_backup_folder()
        
        # Connect to MariaDB
        connection = mariadb.connect(**DB_CONFIG)
        cursor = connection.cursor()

        # Export current data
        timestamp = datetime.now().strftime("%Y%m%d%H%M")
        timestamped_backup = f"{BACKUP_FOLDER}/bookdatabase_load_{timestamp}.txt"
        export_data_to_file(connection, BACKUP_FILE)
        export_data_to_file(connection, timestamped_backup)

        # Drop and recreate the database
        for command in SQL_COMMANDS:
            cursor.execute(command)
            print(f"Executed: {command.strip()[:80]}...")
            print(f"Executed: {command}")

        # Reload data if --reload is used
        if reload_file:
            reload_data_from_file(connection, reload_file)

        print(f"Database {DATABASE_NAME} created successfully!")

    except mariadb.Error as e:
        print(f"Error connecting to MariaDB Platform: {e}")
        sys.exit(1)
    finally:
        # Clean up
        if connection:
            connection.close()

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Manage BookDatabase")
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--empty", action="store_true", help="Export data before recreating the database.")
    group.add_argument("--reload", type=str, help="Reload the database from the specified file.")
    args = parser.parse_args()

    create_database(empty=args.empty, reload_file=args.reload)
