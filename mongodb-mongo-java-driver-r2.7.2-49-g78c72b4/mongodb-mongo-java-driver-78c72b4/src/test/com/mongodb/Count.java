/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mongodb;

import com.mongodb.*;
import java.util.*;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Mohanish
 */

public class Count {

    /*
     * To change this template, choose Tools | Templates and open the template
     * in the editor.
     */
    /**
     *
     * @author Mohanish
     */
    public class count {

        public void main(String args) {

            Mongo m = null;
            try {
                m = new Mongo("localhost", 27017);
            } catch (UnknownHostException ex) {
                Logger.getLogger(count.class.getName()).log(Level.SEVERE, null, ex);
            } catch (MongoException ex) {
                Logger.getLogger(count.class.getName()).log(Level.SEVERE, null, ex);
            }
            DB db = m.getDB("test");
            DBCollection coll = db.getCollection("per");
            // DBCollection coll1 = db.getCollection("or");
            // ArrayList cursorList=new ArrayList();
//        for (int i = 0 ; i<5 ; i++){
//            
//            DBCursor cur = coll.find();
//            
//        }
            System.out.println(coll.getCount());
        }
    }
}
