// DBTCPConnectorTest.java

/**
 *      Copyright (C) 2011 10gen Inc.
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

package com.mongodb;

import com.mongodb.util.TestCase;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.UnknownHostException;

/**
 * Testing functionality of database TCP connector
 */
public class DBTCPConnectorTest extends TestCase {

    @BeforeClass
    public void beforeClass() throws UnknownHostException {
        cleanupMongo = new Mongo("127.0.0.1");
        cleanupDB = "com_mongodb_unittest_DBTCPConnectorTest";
        _db = cleanupMongo.getDB(cleanupDB);
        _collection = _db.getCollection("testCol");
    }

    @BeforeMethod
    public void beforeMethod() throws UnknownHostException {
        _connector = new DBTCPConnector(cleanupMongo, new ServerAddress("127.0.0.1"));
    }

    /**
     * Test request reservation
     */
    @Test
    public void testRequestReservation() {
        assertEquals(false, _connector.getMyPort()._inRequest);
        _connector.requestStart();
        assertNull(_connector.getMyPort()._requestPort);
        assertEquals(true, _connector.getMyPort()._inRequest);
        _connector.requestDone();
        assertEquals(false, _connector.getMyPort()._inRequest);
    }

    /**
     * Tests that same connections is used for sequential writes
     */
    @Test
    public void testConnectionReservationForWrites() {
        _connector.requestStart();
        _connector.say(_db, createOutMessageForInsert(), WriteConcern.SAFE);
        assertNotNull(_connector.getMyPort()._requestPort);
        DBPort requestPort = _connector.getMyPort()._requestPort;
        _connector.say(_db, createOutMessageForInsert(), WriteConcern.SAFE);
        assertEquals(requestPort, _connector.getMyPort()._requestPort);
    }

    /**
     * Tests that same connections is used for write followed by read
     */
    @Test
    public void testConnectionReservationForWriteThenRead() {
        _connector.requestStart();
        _connector.say(_db, createOutMessageForInsert(), WriteConcern.SAFE);
        DBPort requestPort = _connector.getMyPort()._requestPort;
        _connector.call(_db, _collection,
                OutMessage.query(cleanupMongo, 0, _collection.getFullName(), 0, -1, new BasicDBObject(), new BasicDBObject(), ReadPreference.PRIMARY),
                null, 0);
        assertEquals(requestPort, _connector.getMyPort()._requestPort);
    }

    /**
     * Tests that same connections is used for sequential reads
     */
    @Test
    public void testConnectionReservationForReads() {
        _connector.requestStart();
        _connector.call(_db, _collection,
                OutMessage.query(cleanupMongo, 0, _collection.getFullName(), 0, -1, new BasicDBObject(), new BasicDBObject(), ReadPreference.PRIMARY),
                null, 0);
        assertNotNull(_connector.getMyPort()._requestPort);
    }


    private OutMessage createOutMessageForInsert() {
        OutMessage om = new OutMessage( cleanupMongo , 2002, new DefaultDBEncoder() );

        int flags = 0;
        om.writeInt( flags );
        om.writeCString(_collection.getFullName());
        om.putObject( new BasicDBObject() );

        return om;
    }

    private DB _db;
    private DBCollection _collection;
    private DBTCPConnector _connector;
}
