/**
 *      Copyright (C) 2008 10gen Inc.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.BasicDBObject;

public class QuickTourAdmin {
    
    public static void main(String[] args) throws Exception {

        // connect to the local database server 
        Mongo m = new Mongo();

        // Authenticate - optional
        // boolean auth = db.authenticate("foo", "bar");
        
        // get db names
        for (String s : m.getDatabaseNames()) {
            System.out.println(s);
        }


        // get a db
        DB db = m.getDB("com_mongodb_MongoAdmin");

        // do an insert so that the db will really be created.  Calling getDB() doesn't really take any
        // action with the server 
        db.getCollection("testcollection").insert(new BasicDBObject("i",1));
        for (String s : m.getDatabaseNames()) {
            System.out.println(s);
        }

        // drop a database
        m.dropDatabase("com_mongodb_MongoAdmin");

        for (String s : m.getDatabaseNames()) {
            System.out.println(s);
        }        
    }
}
