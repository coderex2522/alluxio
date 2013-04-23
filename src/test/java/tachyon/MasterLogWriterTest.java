package tachyon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.After;
import org.junit.Before;

import org.junit.Test;

import tachyon.conf.MasterConf;

/**
 * Unit tests for tachyon.MasterLogWriter
 */
public class MasterLogWriterTest {
  private LocalTachyonCluster mLocalTachyonCluster = null;
  private MasterLogWriter mMasterLogWriter = null;
  private MasterLogReader mMasterLogReader = null;
  private String mLogFile = null;

  @Before
  public final void before() throws IOException {
    mLocalTachyonCluster = new LocalTachyonCluster(1000);
    mLocalTachyonCluster.start();
    mLogFile = MasterConf.get().LOG_FILE;
    mMasterLogWriter = new MasterLogWriter(mLogFile);
  }

  @After
  public final void after() throws Exception {
    mLocalTachyonCluster.stop();
  }

  @Test
  public void appendAndFlushInodeTest() throws IOException {
    Inode inode = new InodeFile("/testFile", 1, 0);
    mMasterLogWriter.appendAndFlush(inode);
    Inode inode2 = new InodeFolder("/testFolder", 1, 0);
    mMasterLogWriter.appendAndFlush(inode2);
    Inode inode3 = new InodeRawTable("/testRawTable", 1, 0, 1, null);
    mMasterLogWriter.appendAndFlush(inode3);
    mMasterLogReader = new MasterLogReader(mLogFile);
    Assert.assertTrue(mMasterLogReader.hasNext());
    Pair<LogType, Object> toCheck = mMasterLogReader.getNextPair();
    Assert.assertEquals(LogType.InodeFile, toCheck.getFirst());
    Assert.assertEquals(inode, toCheck.getSecond());
    Assert.assertTrue(mMasterLogReader.hasNext());
    toCheck = mMasterLogReader.getNextPair();
    Assert.assertEquals(LogType.InodeFolder, toCheck.getFirst());
    Assert.assertEquals(inode2, toCheck.getSecond());
    Assert.assertTrue(mMasterLogReader.hasNext());
    toCheck = mMasterLogReader.getNextPair();
    Assert.assertEquals(LogType.InodeRawTable, toCheck.getFirst());
    Assert.assertEquals(inode3, toCheck.getSecond());
    Assert.assertFalse(mMasterLogReader.hasNext());
  }

  @Test
  public void appendAndFlushInodeListTest() throws IOException {
    Inode inode = new InodeFile("/testFile", 1, 0);
    Inode inode2 = new InodeFolder("/testFolder", 1, 0);
    Inode inode3 = new InodeRawTable("/testRawTable", 1, 0, 1, null);
    List<Inode> inodeList = new ArrayList<Inode>();
    inodeList.add(inode);
    inodeList.add(inode2);
    inodeList.add(inode3);
    mMasterLogWriter.appendAndFlush(inodeList);
    mMasterLogReader = new MasterLogReader(mLogFile);
    Assert.assertTrue(mMasterLogReader.hasNext());
    Pair<LogType, Object> toCheck = mMasterLogReader.getNextPair();
    Assert.assertEquals(LogType.InodeFile, toCheck.getFirst());
    Assert.assertEquals(inode, toCheck.getSecond());
    Assert.assertTrue(mMasterLogReader.hasNext());
    toCheck = mMasterLogReader.getNextPair();
    Assert.assertEquals(LogType.InodeFolder, toCheck.getFirst());
    Assert.assertEquals(inode2, toCheck.getSecond());
    Assert.assertTrue(mMasterLogReader.hasNext());
    toCheck = mMasterLogReader.getNextPair();
    Assert.assertEquals(LogType.InodeRawTable, toCheck.getFirst());
    Assert.assertEquals(inode3, toCheck.getSecond());
    Assert.assertFalse(mMasterLogReader.hasNext());
  }

  @Test
  public void appendAndFlushCheckpointTest() throws IOException {
    CheckpointInfo checkpointInfo = new CheckpointInfo(1);
    mMasterLogWriter.appendAndFlush(checkpointInfo);
    mMasterLogReader = new MasterLogReader(mLogFile);
    Assert.assertTrue(mMasterLogReader.hasNext());
    Pair<LogType, Object> toCheck = mMasterLogReader.getNextPair();
    Assert.assertEquals(LogType.CheckpointInfo, toCheck.getFirst());
    Assert.assertEquals(checkpointInfo, toCheck.getSecond());
    Assert.assertFalse(mMasterLogReader.hasNext());
  }

  @Test
  public void largeLogTest() throws IOException {
    List<Pair<LogType, Object>> loggedObjects = new ArrayList<Pair<LogType, Object>>(100000);
    Inode inode;
    CheckpointInfo checkpointInfo;
    for (int i = 0; i < 100000; i ++) {
      switch (i % 3) { 
      case 0:
        inode = new InodeFile("/testFile" + i, 1 + i, 0);
        mMasterLogWriter.appendAndFlush(inode);
        loggedObjects.add(new Pair<LogType, Object>(LogType.InodeFile, inode));
        break;
      case 1:
        inode = new InodeFolder("/testFolder" + i, 1 + i, 0);
        mMasterLogWriter.appendAndFlush(inode);
        loggedObjects.add(new Pair<LogType, Object>(LogType.InodeFolder, inode));
        break;
      case 2:
        checkpointInfo = new CheckpointInfo(i);
        mMasterLogWriter.appendAndFlush(checkpointInfo);
        loggedObjects.add(new Pair<LogType, Object>(LogType.CheckpointInfo, checkpointInfo));
        break;
      }
    }
    mMasterLogReader = new MasterLogReader(mLogFile);
    for (Pair<LogType, Object> pair : loggedObjects) {
      Assert.assertEquals(pair, mMasterLogReader.getNextPair());
    }
  }
}
