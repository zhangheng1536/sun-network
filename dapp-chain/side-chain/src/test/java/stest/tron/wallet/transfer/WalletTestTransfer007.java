package stest.tron.wallet.transfer;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.PublicMethedForDailybuild;

@Slf4j
public class WalletTestTransfer007 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethedForDailybuild.getFinalAddress(testKey002);
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethedForDailybuild.getFinalAddress(testKey003);


  private ManagedChannel channelFull = null;
  private ManagedChannel searchChannelFull = null;

  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidityInFullnode = null;

  private WalletGrpc.WalletBlockingStub searchBlockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String searchFullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] sendAccountAddress = ecKey1.getAddress();
  String sendAccountKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private ManagedChannel channelSolidity = null;
  private ManagedChannel channelSolidityInFullnode = null;
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);
  /*  private String solidityInFullnode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(1);*/


  /**
   * constructor.
   */

  @BeforeClass
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    searchChannelFull = ManagedChannelBuilder.forTarget(searchFullnode)
        .usePlaintext(true)
        .build();
    searchBlockingStubFull = WalletGrpc.newBlockingStub(searchChannelFull);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext(true)
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);

    /*    channelSolidityInFullnode = ManagedChannelBuilder.forTarget(solidityInFullnode)
        .usePlaintext(true)
        .build();
    blockingStubSolidityInFullnode = WalletSolidityGrpc.newBlockingStub(channelSolidityInFullnode);
    */
  }


  @Test
  public void testSendCoin() {
    String transactionId = PublicMethedForDailybuild.sendcoinGetTransactionId(sendAccountAddress, 90000000000L,
        fromAddress, testKey002, blockingStubFull);
    Optional<Transaction> infoById = PublicMethedForDailybuild
        .getTransactionById(transactionId, blockingStubFull);
    Long timestamptis = PublicMethedForDailybuild.printTransactionRow(infoById.get().getRawData());
    Long timestamptispBlockOne = PublicMethedForDailybuild.getBlock(1, blockingStubFull).getBlockHeader()
        .getRawData().getTimestamp();
    Assert.assertTrue(timestamptis >= timestamptispBlockOne);
  }

  @Test
  public void testSendCoin2() {
    String transactionId = PublicMethedForDailybuild.sendcoinGetTransactionId(sendAccountAddress, 90000000000L,
        fromAddress, testKey002, blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Optional<Transaction> infoById = PublicMethedForDailybuild
        .getTransactionById(transactionId, blockingStubFull);
    Long timestamptis = PublicMethedForDailybuild.printTransactionRow(infoById.get().getRawData());
    Long timestampBlockOne = PublicMethedForDailybuild.getBlock(1, blockingStubFull).getBlockHeader()
        .getRawData().getTimestamp();
    Assert.assertTrue(timestamptis >= timestampBlockOne);
    PublicMethedForDailybuild.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubSolidity);

    infoById = PublicMethedForDailybuild.getTransactionById(transactionId, blockingStubSolidity);
    timestamptis = PublicMethedForDailybuild.printTransactionRow(infoById.get().getRawData());
    timestampBlockOne = PublicMethedForDailybuild.getBlock(1, blockingStubFull).getBlockHeader()
        .getRawData().getTimestamp();
    Assert.assertTrue(timestamptis >= timestampBlockOne);

    ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(transactionId));
    BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
    TransactionInfo transactionInfo;

    transactionInfo = blockingStubSolidity.getTransactionInfoById(request);
    Assert.assertTrue(transactionInfo.getBlockTimeStamp() >= timestampBlockOne);

    transactionInfo = blockingStubFull.getTransactionInfoById(request);
    Assert.assertTrue(transactionInfo.getBlockTimeStamp() >= timestampBlockOne);

    //transactionInfo = blockingStubSolidityInFullnode.getTransactionInfoById(request);
    //Assert.assertTrue(transactionInfo.getBlockTimeStamp() >= timestampBlockOne);

  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (searchChannelFull != null) {
      searchChannelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }


}
