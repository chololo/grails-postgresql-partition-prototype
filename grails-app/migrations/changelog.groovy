databaseChangeLog = {

	changeSet(author: "jburgess (generated)", id: "1351741955383-1") {
		createTable(tableName: "event") {
			column(name: "id", type: "int8") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "event_pkey")
			}

			column(name: "version", type: "int8") {
				constraints(nullable: "false")
			}

			column(name: "timestamp", type: "TIMESTAMP WITH TIME ZONE") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "jburgess (generated)", id: "1351741955383-2") {
		createSequence(schemaName: "public", sequenceName: "hibernate_sequence")
	}
	
	// Create partition tables
	changeSet(author: "jburgess (generated)", id: "1351741955383-3") {
		
		grailsChange 
		{
			change
			{
				(1989..2020).each 
				{
					year ->
					
					def tableName = "event_" + year
					def sqlStatement =
					"CREATE TABLE " + tableName + " (CHECK (timestamp >= '" + year + "-01-01' AND timestamp < '" + (year + 1) + "-01-01' )) INHERITS (event);"

					println "Executing sql: ${sqlStatement}"
					sql.execute(sqlStatement)
					
					sql.execute("CREATE INDEX event_" + year + "_timestamp ON event_" + year + " (timestamp);")
				}
			}
		}
	}
	
	// Create insert trigger
	changeSet(author: "jburgess (generated)", id: "1351741955383-4") {
		
		grailsChange
		{
			change
			{
				def query = '''CREATE OR REPLACE FUNCTION event_insert_trigger()
RETURNS TRIGGER AS $$
BEGIN
'''
				(1989..2020).each
				{
					year ->
					
					if (year == 1989)
					{
						query += "IF "
					}
					else
					{
						query += "ELSIF "
					}
					
					query += "( NEW.timestamp >= '" + year + "-01-01' AND NEW.timestamp < '" + (year + 1) + "-01-01' ) THEN INSERT INTO event_" + year + " VALUES (NEW.*);"
				}
				
				query += '''
    ELSE
        RAISE EXCEPTION 'Timestamp out of range.  Fix the event_insert_trigger() function!';
    END IF;
    RETURN NEW;
END;
$$
LANGUAGE plpgsql;'''
				
				sql.execute(query)
				
				sql.execute("CREATE TRIGGER event_insert_trigger BEFORE INSERT ON event FOR EACH ROW EXECUTE PROCEDURE event_insert_trigger();")
			}
		}
	}
	
	// Create delete trigger (to avoid duplicates in base table)
	changeSet(author: "jburgess (generated)", id: "1351741955383-5") {
		
		grailsChange
		{
			change
			{
				def query = '''CREATE OR REPLACE FUNCTION event_delete_master() RETURNS trigger
									    AS $$
									DECLARE
									    r event%rowtype;
									BEGIN
									    DELETE FROM ONLY event where id = new.id returning * into r;
									    RETURN r;
									end;
									$$
									LANGUAGE plpgsql;'''
				
				sql.execute(query)
				sql.execute('''create trigger after_insert_event_trigger 
								    after insert on event
								    for each row
								        execute procedure event_delete_master();''')
			}
		}
	}
}
