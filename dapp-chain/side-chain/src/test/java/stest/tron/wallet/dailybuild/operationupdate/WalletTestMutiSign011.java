package stest.tron.wallet.dailybuild.operationupdate;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethedForDailybuild;
import stest.tron.wallet.common.client.utils.PublicMethedForMutiSign;

@Slf4j
public class WalletTestMutiSign011 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethedForDailybuild.getFinalAddress(testKey002);
  private final String testKey001 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress01 = PublicMethedForDailybuild.getFinalAddress(testKey001);
  private long multiSignFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.multiSignFee");
  private long updateAccountPermissionFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.updateAccountPermissionFee");
  private final String operations = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.operations");
  private static final long now = System.currentTimeMillis();
  private static String name = "MutiSign001_" + Long.toString(now);
  private static final long totalSupply = now;
  String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  ByteString assetAccountId1;
  String[] permissionKeyString = new String[2];
  String[] ownerKeyString = new String[2];
  String accountPermissionJson = "";

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] manager1Address = ecKey1.getAddress();
  String manager1Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] manager2Address = ecKey2.getAddress();
  String manager2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] ownerAddress = ecKey3.getAddress();
  String ownerKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());

  ECKey ecKey4 = new ECKey(Utils.getRandom());
  byte[] participateAddress = ecKey4.getAddress();
  String participateKey = ByteArray.toHexString(ecKey4.getPrivKeyBytes());

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * constructor.
   */

  @BeforeClass(enabled = false)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test(enabled = false)
  public void testMutiSign1UpdatePermission() {
    ecKey1 = new ECKey(Utils.getRandom());
    manager1Address = ecKey1.getAddress();
    manager1Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    ecKey2 = new ECKey(Utils.getRandom());
    manager2Address = ecKey2.getAddress();
    manager2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

    ecKey3 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey3.getAddress();
    ownerKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
    PublicMethedForDailybuild.printAddress(ownerKey);

    long needCoin = updateAccountPermissionFee * 2 + multiSignFee * 3;

    Assert.assertTrue(
        PublicMethedForDailybuild.sendcoin(ownerAddress, needCoin, fromAddress, testKey002,
            blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethedForDailybuild.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceBefore: " + balanceBefore);

    permissionKeyString[0] = manager1Key;
    permissionKeyString[1] = manager2Key;
    ownerKeyString[0] = ownerKey;
    ownerKeyString[1] = manager1Key;
    String[] permissionKeyString1 = new String[1];
    permissionKeyString1[0] = ownerKey;
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(ownerKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":2,"
            + "\"operations\":\"" + operations + "\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(manager2Key) + "\",\"weight\":1}"
            + "]}]}";

    logger.info(accountPermissionJson);
    String txid = PublicMethedForMutiSign
        .accountPermissionUpdateForTransactionId(accountPermissionJson, ownerAddress, ownerKey,
            blockingStubFull, ownerKeyString);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = PublicMethedForDailybuild
        .getTransactionInfoById(txid, blockingStubFull);

    long balanceAfter = PublicMethedForDailybuild.queryAccount(ownerAddress, blockingStubFull).getBalance();
    long energyFee = infoById.get().getReceipt().getEnergyFee();
    long netFee = infoById.get().getReceipt().getNetFee();
    long fee = infoById.get().getFee();

    logger.info("balanceAfter: " + balanceAfter);
    logger.info("energyFee: " + energyFee);
    logger.info("netFee: " + netFee);
    logger.info("fee: " + fee);

    Assert.assertEquals(balanceBefore - balanceAfter, fee);
    Assert.assertEquals(fee, energyFee + netFee + updateAccountPermissionFee);

    balanceBefore = balanceAfter;
    String accountPermissionJson1 =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(ownerKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":2,"
            + "\"operations\":\"" + operations + "\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(ownerKey) + "\",\"weight\":1}"
            + "]}]}";
    txid = PublicMethedForMutiSign
        .accountPermissionUpdateForTransactionId1(accountPermissionJson1, ownerAddress, ownerKey,
            blockingStubFull, 2, permissionKeyString);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Assert.assertNotNull(txid);

    infoById = PublicMethedForDailybuild
        .getTransactionInfoById(txid, blockingStubFull);
    balanceAfter = PublicMethedForDailybuild.queryAccount(ownerAddress, blockingStubFull).getBalance();
    energyFee = infoById.get().getReceipt().getEnergyFee();
    netFee = infoById.get().getReceipt().getNetFee();
    fee = infoById.get().getFee();

    logger.info("balanceAfter: " + balanceAfter);
    logger.info("energyFee: " + energyFee);
    logger.info("netFee: " + netFee);
    logger.info("fee: " + fee);


  }


  /**
   * constructor.
   */
  @AfterClass(enabled = false)
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


