/* Copyright 2001, 2019 IBM Corporation
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the 
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the 
 * following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the 
 * following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, 
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE 
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
 package com.ibm.bluematter.utils;

import java.sql.*;
import java.text.*;
import java.io.*;
import java.util.*;

public class Impl2db2 {

    private static Statement stmt = null;
    private static Statement stmt1 = null;
    private static boolean debug = false;

    static {
      try {
         //  register the driver with DriverManager 
         //  The newInstance() call is needed for the sample to work with 
         //  JDK 1.1.1 on OS/2, where the Class.forName() method does not 
         //  run the static initializer. For other JDKs, the newInstance 
         //  call can be omitted. 
         Class.forName("COM.ibm.db2.jdbc.app.DB2Driver").newInstance();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }    

/**
*   The main program sets up all the neccesary tables for  probspecgen
*/
   public static void main(String argv[]) {

      int argc = argv.length;
      if(argc < 1 || argv[0].equals("-usage") ) {
    usage();
    return;
      }

      String implFile = argv[0];

      if(!implFile.endsWith(".impl") || implFile.equals("")) {
          System.out.println("The first argument does not end in .impl ");
          System.exit(1);
      }
      
      String databaseName = new String("mdsetup");
      String implDesc      = null;

      int i=1;
      while (i < argc) {
    if( argv[i].equals("-database")) {
        if((i != argc-1) || argv[i+1].startsWith("-")) {
      databaseName = argv[i+1];
        }
        else {
      usage();
      return;
        }
    }
          else if( argv[i].equals("-desc")) {
        if((i != argc-1) || argv[i+1].startsWith("-")) {
      implDesc = argv[i+1];
        }
        else {
      usage();
      return;
        }
          }
          else if( argv[i].equals("-debug")) {
              debug=true;
          }
    i++;
      }      

      try {
          SimpleImplParser parser = new SimpleImplParser( implFile );
          Hashtable impl = parser.parse();

          // If parsing of the file went well, then create a new impl_id
          String implFilename = implFile.substring(implFile.lastIndexOf("/")+1, implFile.lastIndexOf(".impl"));
          
          //  connect with default id/password 
          Connection con = DriverManager.getConnection( "jdbc:db2:" + databaseName );
          stmt = con.createStatement();
          stmt1 = con.createStatement();
          
          Runtime runTime = Runtime.getRuntime();
          Process p1 = runTime.exec("whoami");
          BufferedReader in = new BufferedReader(new InputStreamReader(p1.getInputStream()));
          
          String creator = (in.readLine()).trim();
          in.close();
          
          if(creator == null) {
              System.out.println("ERROR: Program is not able to establish creator");
              System.exit(1);
          }

          Timestamp now = new Timestamp((new java.util.Date()).getTime());
          String timeString = now.toString();

          if(implDesc != null)
              stmt.executeUpdate("INSERT INTO EXPERIMENT.impl_table ( filename,desc,creator,created ) "
                                 +" VALUES ('" + implFilename + "','"+implDesc+"','"+creator+"','"+timeString+"')");
          else
              stmt.executeUpdate("INSERT INTO EXPERIMENT.impl_table ( filename,creator,created ) "
                                 +" VALUES ('" + implFilename + "','"+creator+"','"+timeString+"')");
          
          
          ResultSet rs = stmt.executeQuery("SELECT impl_id FROM EXPERIMENT.impl_table "
                                           + "WHERE filename='"+implFilename+"'"
                                           + " AND creator='"+creator+"'"
                                           + " AND created='"+timeString+"'" );

          if(!rs.next())
              throw new Exception ("ERROR: Problem occured while retrieving the compiler time parameters from db2");
          
          int impl_id = rs.getInt(1);

          // impl hashtable contains the mapping of name --> value to be put into db2
          Enumeration keys = impl.keys();          
          while(keys.hasMoreElements()) {
              String key = ((String) keys.nextElement()).trim();
              String value = ((String) impl.get(key)).trim();
              String literal = (value.substring(value.lastIndexOf(":")+1, value.length())).trim();
              value = value.substring(0,value.lastIndexOf(":"));
   
               System.out.println("Key: "+ key);
               System.out.println("Value: "+ value);
              
              stmt.executeUpdate("INSERT INTO EXPERIMENT.impl_params (impl_id,name,value,literal) "
                                      + " VALUES ("+impl_id+",'"+key+"','"+value+"','"+literal+"')");                                
          }

          System.out.println("IMPL_ID: " + impl_id);
      }
      catch (Exception e) {
          System.out.println(e.getMessage());
          e.printStackTrace();
      }      
   }
    
   private static void usage() {
       System.out.println("Input: <impl_filename> [flags]");
       System.out.println("flags: -database <database_name> --> \"mdsetup\" by default");
       return;
   }
}