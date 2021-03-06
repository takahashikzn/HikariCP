/*
 * Copyright (C) 2013 Brett Wooldridge
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zaxxer.hikari;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import com.zaxxer.hikari.mocks.StubConnection;
import com.zaxxer.hikari.mocks.StubStatement;
import com.zaxxer.hikari.pool.HikariPool;
import com.zaxxer.hikari.util.UtilityElf;

/**
 * System property testProxy can be one of:
 *    "com.zaxxer.hikari.JavaProxyFactory"
 *    "com.zaxxer.hikari.CglibProxyFactory"
 *    "com.zaxxer.hikari.JavassistProxyFactory"
 *
 * @author Brett Wooldridge
 */
public class TestConnections
{
   @SuppressWarnings("deprecation")
   @Test
   public void testCreate() throws SQLException
   {
      HikariConfig config = new HikariConfig();
      config.setMinimumIdle(1);
      config.setMaximumPoolSize(1);
      config.setConnectionTestQuery("VALUES 1");
      config.setConnectionInitSql("SELECT 1");
      config.setReadOnly(true);
      config.setLeakDetectionThreshold(TimeUnit.SECONDS.toMillis(60));
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      HikariDataSource ds = new HikariDataSource(config);
      ds.setLoginTimeout(10);
      Assert.assertSame(10, ds.getLoginTimeout());
      try {
         Assert.assertSame("Totals connections not as expected", 1, TestElf.getPool(ds).getTotalConnections());
         Assert.assertSame("Idle connections not as expected", 1, TestElf.getPool(ds).getIdleConnections());

         Connection connection = ds.getConnection();
         Assert.assertNotNull(connection);

         Assert.assertSame("Totals connections not as expected", 1, TestElf.getPool(ds).getTotalConnections());
         Assert.assertSame("Idle connections not as expected", 0, TestElf.getPool(ds).getIdleConnections());

         PreparedStatement statement = connection.prepareStatement("SELECT * FROM device WHERE device_id=?");
         Assert.assertNotNull(statement);

         statement.setInt(1, 0);

         ResultSet resultSet = statement.executeQuery();
         Assert.assertNotNull(resultSet);

         Assert.assertFalse(resultSet.next());

         resultSet.close();
         statement.close();
         connection.close();

         Assert.assertSame("Totals connections not as expected", 1, TestElf.getPool(ds).getTotalConnections());
         Assert.assertSame("Idle connections not as expected", 1, TestElf.getPool(ds).getIdleConnections());
      }
      finally {
         ds.close();
      }
   }

   @Test
   public void testMaxLifetime() throws Exception
   {
      HikariConfig config = new HikariConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(1);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      System.setProperty("com.zaxxer.hikari.housekeeping.periodMs", "100");

      HikariDataSource ds = new HikariDataSource(config);

      try {
         System.clearProperty("com.zaxxer.hikari.housekeeping.periodMs");

         ds.setMaxLifetime(700);

         Assert.assertSame("Total connections not as expected", 0, TestElf.getPool(ds).getTotalConnections());
         Assert.assertSame("Idle connections not as expected", 0, TestElf.getPool(ds).getIdleConnections());

         Connection connection = ds.getConnection();
         Connection unwrap = connection.unwrap(Connection.class);
         Assert.assertNotNull(connection);

         Assert.assertSame("Second total connections not as expected", 1, TestElf.getPool(ds).getTotalConnections());
         Assert.assertSame("Second idle connections not as expected", 0, TestElf.getPool(ds).getIdleConnections());
         connection.close();

         Assert.assertSame("Idle connections not as expected", 1, TestElf.getPool(ds).getIdleConnections());

         Connection connection2 = ds.getConnection();
         Connection unwrap2 = connection2.unwrap(Connection.class);
         Assert.assertSame(unwrap, unwrap2);
         Assert.assertSame("Second total connections not as expected", 1, TestElf.getPool(ds).getTotalConnections());
         Assert.assertSame("Second idle connections not as expected", 0, TestElf.getPool(ds).getIdleConnections());
         connection2.close();

         Thread.sleep(2000);

         connection2 = ds.getConnection();
         Assert.assertNotSame("Expected a different connection", connection, connection2);

         connection2.close();

         Assert.assertSame("Post total connections not as expected", 1, TestElf.getPool(ds).getTotalConnections());
         Assert.assertSame("Post idle connections not as expected", 1, TestElf.getPool(ds).getIdleConnections());
      }
      finally {
         ds.close();
         System.getProperties().remove("com.zaxxer.hikari.housekeeping.periodMs");
      }
   }

   @Test
   public void testMaxLifetime2() throws Exception
   {
      HikariConfig config = new HikariConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(1);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      HikariDataSource ds = new HikariDataSource(config);

      try {
         ds.setMaxLifetime(700);

         Assert.assertSame("Total connections not as expected", 0, TestElf.getPool(ds).getTotalConnections());
         Assert.assertSame("Idle connections not as expected", 0, TestElf.getPool(ds).getIdleConnections());

         Connection connection = ds.getConnection();
         Connection unwrap = connection.unwrap(Connection.class);
         Assert.assertNotNull(connection);

         Assert.assertSame("Second total connections not as expected", 1, TestElf.getPool(ds).getTotalConnections());
         Assert.assertSame("Second idle connections not as expected", 0, TestElf.getPool(ds).getIdleConnections());
         connection.close();

         Assert.assertSame("Idle connections not as expected", 1, TestElf.getPool(ds).getIdleConnections());

         Connection connection2 = ds.getConnection();
         Connection unwrap2 = connection2.unwrap(Connection.class);
         Assert.assertSame(unwrap, unwrap2);
         Assert.assertSame("Second total connections not as expected", 1, TestElf.getPool(ds).getTotalConnections());
         Assert.assertSame("Second idle connections not as expected", 0, TestElf.getPool(ds).getIdleConnections());
         connection2.close();

         Thread.sleep(800);

         connection2 = ds.getConnection();
         Assert.assertNotSame("Expected a different connection", connection, connection2);

         connection2.close();

         Assert.assertSame("Post total connections not as expected", 1, TestElf.getPool(ds).getTotalConnections());
         Assert.assertSame("Post idle connections not as expected", 1, TestElf.getPool(ds).getIdleConnections());
      }
      finally {
         ds.close();
      }
   }

   @Test
   public void testDoubleClose() throws Exception
   {
      HikariConfig config = new HikariConfig();
      config.setMinimumIdle(1);
      config.setMaximumPoolSize(1);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      HikariDataSource ds = new HikariDataSource(config);
      try {
         Connection connection = ds.getConnection();
         connection.close();
         connection.close();
      }
      finally {
         ds.close();
      }
   }

   @Test
   public void testEviction() throws Exception
   {
      HikariConfig config = new HikariConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(5);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      HikariDataSource ds = new HikariDataSource(config);
      try {
         Connection connection = ds.getConnection();
         Assert.assertEquals(1, TestElf.getPool(ds).getTotalConnections());
         ds.evictConnection(connection);
         Assert.assertEquals(0, TestElf.getPool(ds).getTotalConnections());
      }
      finally {
         ds.close();
      }
   }

   @Test
   public void testBackfill() throws Exception
   {
      HikariConfig config = new HikariConfig();
      config.setMinimumIdle(1);
      config.setMaximumPoolSize(4);
      config.setConnectionTimeout(500);
      config.setInitializationFailFast(false);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      HikariDataSource ds = new HikariDataSource(config);
      try {
         UtilityElf.quietlySleep(1200L);

         Assert.assertSame("Totals connections not as expected", 1, TestElf.getPool(ds).getTotalConnections());
         Assert.assertSame("Idle connections not as expected", 1, TestElf.getPool(ds).getIdleConnections());

         // This will take the pool down to zero
         Connection connection = ds.getConnection();
         Assert.assertNotNull(connection);

         Assert.assertSame("Totals connections not as expected", 1, TestElf.getPool(ds).getTotalConnections());
         Assert.assertSame("Idle connections not as expected", 0, TestElf.getPool(ds).getIdleConnections());

         PreparedStatement statement = connection.prepareStatement("SELECT some, thing FROM somewhere WHERE something=?");
         Assert.assertNotNull(statement);

         ResultSet resultSet = statement.executeQuery();
         Assert.assertNotNull(resultSet);

         try {
            statement.getMaxFieldSize();
            Assert.fail();
         }
         catch (Exception e) {
            Assert.assertSame(SQLException.class, e.getClass());
         }

         // The connection will be ejected from the pool here
         connection.close();

         Assert.assertSame("Totals connections not as expected", 0, TestElf.getPool(ds).getTotalConnections());
         Assert.assertSame("Idle connections not as expected", 0, TestElf.getPool(ds).getIdleConnections());

         // This will cause a backfill
         connection = ds.getConnection();
         connection.close();

         Assert.assertTrue("Totals connections not as expected", TestElf.getPool(ds).getTotalConnections() > 0);
         Assert.assertTrue("Idle connections not as expected", TestElf.getPool(ds).getIdleConnections() > 0);
      }
      finally {
         ds.close();
      }
   }

   @Test
   public void testMaximumPoolLimit() throws Exception
   {
      HikariConfig config = new HikariConfig();
      config.setMinimumIdle(1);
      config.setMaximumPoolSize(4);
      config.setConnectionTimeout(TimeUnit.MINUTES.toMillis(1));
      config.setInitializationFailFast(true);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      StubConnection.count.set(0);

      final HikariDataSource ds = new HikariDataSource(config);
      try {
         Thread[] threads = new Thread[20];
         for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(new Runnable() {
               public void run()
               {
                  try {
                     Connection connection = ds.getConnection();
                     Thread.sleep(500);
                     connection.close();
                  }
                  catch (Exception e) {
                     e.printStackTrace();
                  }
               }
            });
         }

         for (int i = 0; i < threads.length; i++) {
            threads[i].start();
         }

         for (int i = 0; i < threads.length; i++) {
            threads[i].join();
         }

         Assert.assertEquals(4, StubConnection.count.get());
      }
      finally {
         ds.close();
      }
   }

   @SuppressWarnings("deprecation")
   @Test
   public void testGetWithUsername() throws Exception
   {
      HikariConfig config = new HikariConfig();
      config.setMinimumIdle(0);
      config.setMaximumPoolSize(4);
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      final HikariDataSource ds = new HikariDataSource(config);
      try {
         Connection connection1 = ds.getConnection("foo", "bar");
         connection1.close();

         Connection connection2 = ds.getConnection("foo", "bar");
         connection2.close();

         Assert.assertSame(connection1.unwrap(Connection.class), connection2.unwrap(Connection.class));

         Connection connection3 = ds.getConnection("faz", "baf");
         connection3.close();

         HashMap<Object, HikariPool> multiPool = TestElf.getMultiPool(ds);
         Assert.assertTrue(multiPool.size() > 1);

         Object[] array = multiPool.keySet().toArray();
         Assert.assertNotEquals(array[0], array[1]);
      }
      finally {
         ds.close();
      }
   }

   @Test
   public void testOldDriver() throws Exception
   {
      HikariConfig config = new HikariConfig();
      config.setMinimumIdle(1);
      config.setMaximumPoolSize(1);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      StubConnection.oldDriver = true;
      StubStatement.oldDriver = true;
      HikariDataSource ds = new HikariDataSource(config);
      try {
         UtilityElf.quietlySleep(1001L);

         Connection connection = ds.getConnection();
         connection.close();

         UtilityElf.quietlySleep(1001L);
         connection = ds.getConnection();
      }
      finally {
         StubConnection.oldDriver = false;
         StubStatement.oldDriver = false;
         ds.close();
      }
   }

   @Test
   public void testSuspendResume() throws Exception
   {
      HikariConfig config = new HikariConfig();
      config.setMinimumIdle(3);
      config.setMaximumPoolSize(3);
      config.setAllowPoolSuspension(true);
      config.setConnectionTestQuery("VALUES 1");
      config.setDataSourceClassName("com.zaxxer.hikari.mocks.StubDataSource");

      final HikariDataSource ds = new HikariDataSource(config);
      try {
         HikariPool pool = TestElf.getPool(ds);
         while (pool.getTotalConnections() < 3) {
            UtilityElf.quietlySleep(50);
         }

         Thread t = new Thread(new Runnable() {
            public void run()
            {
               try {
                  ds.getConnection();
                  ds.getConnection();
               }
               catch (Exception e) {
                  Assert.fail();
               }
            }
         });

         Connection c3 = ds.getConnection();
         Assert.assertEquals(2, pool.getIdleConnections());

         pool.suspendPool();
         t.start();

         UtilityElf.quietlySleep(500);
         Assert.assertEquals(2, pool.getIdleConnections());
         c3.close();
         Assert.assertEquals(3, pool.getIdleConnections());
         pool.resumePool();
         UtilityElf.quietlySleep(500);
         Assert.assertEquals(1, pool.getIdleConnections());
      }
      finally {
         ds.close();
      }
   }
}
