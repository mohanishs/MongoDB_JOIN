/*
 * Joins.java is a class which implements joins for mongoDB collections.
 */
package com.mongodb;

import java.util.*;
import java.net.*;

public class Joins {
    /*
     * The Join Method performs the task of joining mongoDB collection on a
     * given common column name
     */
    public static String Join(String dataBaseName, String collectionName1, String collectionName2, String columnName) throws UnknownHostException {

        try {

            int columnCount1 = 0, columnCount2 = 0; //Count for number of Columns
            Mongo m = new Mongo("localhost", 27017); //MongoDB Connection
            DB db = m.getDB(dataBaseName); // Getting the specified DB
            DBCollection mongoCollection1 = db.getCollection(collectionName1); // Getting Col 1
            DBCollection mongoCollection2 = db.getCollection(collectionName2); // Getting Col 2

            // Extracting the row names for column 1, using delimiters
            DBObject collection1Document = mongoCollection1.findOne(); // read first row in collection
            String document1String = collection1Document.toString(); //convert first row to string
            String attributeArray1[] = document1String.split(","); // split all words in first row

            //To read and store column names
            Vector<String> column1Attributes = new Vector<String>();
            for (int i = 1; i < attributeArray1.length; i++) {
                String temp = attributeArray1[i].substring(2, (attributeArray1[i].indexOf('"', 2)));
                column1Attributes.add(temp);
                columnCount1++; // to increment col in first collection
            }

            // Extracting the row names for column 2, using delimiters
            DBObject collection2Document = mongoCollection2.findOne();
            String document2String = collection2Document.toString();
            String attributeArray2[] = document2String.split(",");

            //To read and store column2 names
            Vector<String> column2Attributes = new Vector<String>();
            for (int i = 1; i < attributeArray2.length; i++) {
                String temp = attributeArray2[i].substring(2, (attributeArray2[i].indexOf('"', 2)));
                column2Attributes.add(temp);
                columnCount2++; // to increment column2 count
            }

            // Extracting all the collection1 values into a 2-dimensional array
            DBCursor column1Cursor = mongoCollection1.find();
            String collection1[][] = new String[(int) mongoCollection1.getCount()][columnCount1];
            int col1Index = 0;
            while (column1Cursor.hasNext()) {
                for (int i = 0; i < columnCount1; i++) {
                    if (i == 0) {
                        collection1[col1Index][i] = (column1Cursor.next().get(column1Attributes.get(i))).toString();
                    } else {
                        collection1[col1Index][i] = (column1Cursor.curr().get(column1Attributes.get(i))).toString();
                    }
                }
                col1Index++;
            }

            // Extracting all the collection2 values into a 2-dimensional array
            DBCursor column2Cursor = mongoCollection2.find();
            String collection2[][] = new String[(int) mongoCollection2.getCount()][columnCount2];
            int col2Index = 0;
            while (column2Cursor.hasNext()) {
                for (int i = 0; i < columnCount2; i++) {
                    if (i == 0) {
                        collection2[col2Index][i] = (column2Cursor.next().get(column2Attributes.get(i))).toString();
                    } else {
                        collection2[col2Index][i] = (column2Cursor.curr().get(column2Attributes.get(i))).toString();
                    }
                }
                col2Index++;
            }

            // Checking the column on which join is performed
            int flagColumnPresent1 = 0, flagColumnPresent2 = 0;
            int columnPosition1 = 0, columnPosition2 = 0;
            // Checking for the key column number for collection1
            for (int findCol = 0; findCol < column1Attributes.size(); findCol++) {
                if (columnName.equals(column1Attributes.get(findCol))) {
                    columnPosition1 = findCol;
                    flagColumnPresent1 = 1;
                }
            }
            // Checking for the key column number for collection1
            for (int findCol1 = 0; findCol1 < column2Attributes.size(); findCol1++) {
                if (columnName.equals(column2Attributes.get(findCol1))) {
                    columnPosition2 = findCol1;
                    flagColumnPresent2 = 1;
                }
            }
            // If common column not present in both, exception is thrown
            if (flagColumnPresent1 != 1 || flagColumnPresent2 != 1) {
                throw new Exception();
            }

            // Performing the join using collections and result in 3rd array
            String collection1Key = " ";
            String collection2Key = " ";
            String result[][] = new String[(int) (mongoCollection1.getCount() * mongoCollection2.getCount())]
                    [(columnCount1 + columnCount2) - 1];
            int index1flag = 0;
            //Traversing through collection1
            for (int k = 0; k < mongoCollection1.getCount(); k++) {
                collection1Key = collection1[k][columnPosition1];
                //Traversing through collection2
                for (int l = 0; l < mongoCollection2.getCount(); l++) {
                    collection2Key = collection2[l][columnPosition2];
                    // If values match, join is performed
                    if (collection1Key.equals(collection2Key)) {
                        int index2flag = columnCount1;
                        for (int t = 0; t < columnCount1; t++) {
                            //collection1 values in result
                            result[index1flag][t] = collection1[k][t];
                        }
                        for (int x = 0; x < (columnCount2 - 1); x++) {
                            //collection2 values in result
                            result[index1flag][index2flag] = collection2[l][x];
                            index2flag++;
                        }
                        index1flag++;
                    }
                }
            }

            // Vector with all the attribute names
            Vector<String> allAttributes = new Vector<String>();
            for (int attribute1 = 0; attribute1 < columnCount1; attribute1++) {

                allAttributes.add(column1Attributes.get(attribute1));
            }
            for (int attribute2 = 0; attribute2 < columnCount2; attribute2++) {

                allAttributes.add(column2Attributes.get(attribute2));
            }

            // Inserting the join result as a collection in MongoDB
            String New_Collection = collectionName1 + "_" + collectionName2;
            
            
            //Creating new collection in MongoDB
            DBCollection coll_Join = db.getCollection(New_Collection);
     
            //Creating a Document to insert in the Collection       
            BasicDBObject doc = new BasicDBObject();
            int breakFlag = 0;
            int cursorFlag = 0;
            for (int row = 0; row < (mongoCollection1.getCount() * mongoCollection2.getCount()); row++) {
                if (breakFlag == 1) {
                    break;
                }
                for (int cursor = 0; cursor < ((columnCount1 + columnCount2) - 1); cursor++) {
                    if (result[cursorFlag][cursor] != null) {
                        //adding into the document
                        doc.put(allAttributes.get(cursor), result[cursorFlag][cursor]);
                    } else {
                        breakFlag = 1;
                        break;
                    }
                }
                if (breakFlag != 1) {
                    // inserting the document in the collection
                    coll_Join.insert(doc);
                }
                doc.clear(); // reset the doc to use again
                cursorFlag++;
            }

        } catch (Exception e) {
            return ("****ONE OF THE ENTRIES IS INCORRECT*****");
        }
        System.out.println("SUCCESS");
        return "SUCCESS";
    }

    public static void main(String[] args){
    }
}
