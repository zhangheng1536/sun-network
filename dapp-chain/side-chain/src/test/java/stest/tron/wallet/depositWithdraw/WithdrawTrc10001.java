package stest.tron.wallet.depositWithdraw;

import static org.tron.api.GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR;
import static org.tron.protos.Protocol.TransactionInfo.code.FAILED;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.spongycastle.util.encoders.Hex;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.Return;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.AbiUtil;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class WithdrawTrc10001 {


  private final String testDepositTrx = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testDepositAddress = PublicMethed.getFinalAddress(testDepositTrx);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private ManagedChannel channelSolidity = null;

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingSideStubFull = null;
  ByteString assetAccountId;

  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;

  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("mainfullnode.ip.list").get(0);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);

  private final String testKeyFordeposit = Configuration.getByPath("testng.conf")
      .getString("mainNetAssetAccountKey.key3");

  private final byte[] depositAddress = PublicMethed.getFinalAddress(testKeyFordeposit);


  private final String testKeyFordeposit2 = Configuration.getByPath("testng.conf")
      .getString("mainNetAssetAccountKey.key4");

  private final byte[] depositAddress2 = PublicMethed.getFinalAddress(testKeyFordeposit2);
  final String sideGatewayAddress = Configuration.getByPath("testng.conf")
      .getString("gateway_address.key2");
  final String mainGateWayAddress = Configuration.getByPath("testng.conf")
      .getString("gateway_address.key1");

  final String ChainIdAddress = Configuration.getByPath("testng.conf")
      .getString("gateway_address.chainIdAddress");
  final byte[] ChainIdAddressKey = WalletClient.decodeFromBase58Check(ChainIdAddress);

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(testKeyFordeposit);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingSideStubFull = WalletGrpc.newBlockingStub(channelFull1);
  }

  @Test(enabled = true, description = "Withdraw Trc10 normal and Withdraw Trc10 with account exception.")
  public void test1WithdrawTrc10001() {

    Assert.assertTrue(PublicMethed
        .sendcoin(depositAddress, 3100_000_000L, testDepositAddress, testDepositTrx,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account accountMainBefore = PublicMethed.queryAccount(depositAddress, blockingStubFull);
    long accountMainBalance = accountMainBefore.getBalance();
    assetAccountId = PublicMethed
        .queryAccount(depositAddress, blockingStubFull).getAssetIssuedID();
    logger.info("The token ID: " + assetAccountId.toStringUtf8());
    Long depositMainTokenBefore = PublicMethed
        .getAssetIssueValue(depositAddress, assetAccountId, blockingStubFull);
    Long depositSideTokenBefore = PublicMethed
        .getAssetIssueValue(depositAddress, assetAccountId, blockingSideStubFull);

    Account accountSideBefore = PublicMethed.queryAccount(depositAddress, blockingSideStubFull);
    long accountSideBeforeBalance = accountSideBefore.getBalance();
    ByteString address = accountSideBefore.getAddress();
    String accountSideBeforeAddress = Base58.encode58Check(address.toByteArray());
    logger.info("accountSideBeforeAddress:" + accountSideBeforeAddress);
    Assert.assertEquals("3QJmnh", accountSideBeforeAddress);

    logger.info("accountBeforeBalance:" + accountMainBalance);
    logger.info("accountSideBeforeBalance:" + accountSideBeforeBalance);
    logger.info("depositMainTokenBefore:" + depositMainTokenBefore);
    logger.info("depositSideAddressTokenBefore:" + depositSideTokenBefore);
    Assert.assertTrue(depositSideTokenBefore == 0);

    String methodStr = "depositTRC10(uint64,uint64)";

    String inputTokenID = assetAccountId.toStringUtf8();
    long inputTokenValue = 10;
    String inputParam = inputTokenID + "," + inputTokenValue;
    byte[] input = Hex.decode(AbiUtil.parseMethod(methodStr, inputParam, false));
    String txid = PublicMethed
        .triggerContract(WalletClient.decodeFromBase58Check(mainGateWayAddress),
            0,
            input,
            maxFeeLimit, inputTokenValue, inputTokenID, depositAddress, testKeyFordeposit,
            blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingSideStubFull);

    Optional<TransactionInfo> infoById;
    infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    long fee = infoById.get().getFee();
    logger.info("fee:" + fee);
    Account accountMainAfter = PublicMethed.queryAccount(depositAddress, blockingStubFull);
    long accountMainAfterBalance = accountMainAfter.getBalance();
    logger.info("accountMainAfterBalance:" + accountMainAfterBalance);
    Assert.assertEquals(accountMainAfterBalance, accountMainBalance - fee);
    Account accountSideAfter = PublicMethed.queryAccount(depositAddress, blockingSideStubFull);
    long accountSideAfterBalance = accountSideAfter.getBalance();
    ByteString addressSideAfter = accountSideAfter.getAddress();
    String accountSideAfterAddress = Base58.encode58Check(addressSideAfter.toByteArray());
    logger.info("accountSideAfterAddress:" + accountSideAfterAddress);
    Assert.assertEquals(Base58.encode58Check(depositAddress), accountSideAfterAddress);
    Assert.assertEquals(0, accountSideAfterBalance);
    Long depositSideTokenAfter = PublicMethed
        .getAssetIssueValue(depositAddress, assetAccountId, blockingSideStubFull);
    Assert.assertTrue(inputTokenValue == depositSideTokenAfter);
    Long depositMainTokenAfter = PublicMethed
        .getAssetIssueValue(depositAddress, assetAccountId, blockingStubFull);
    Assert.assertTrue(depositMainTokenBefore - inputTokenValue == depositMainTokenAfter);
    logger.info("depositMainTokenAfter:" + depositMainTokenAfter);
    logger.info("depositSideTokenAfter:" + depositSideTokenAfter);
    Assert.assertTrue(depositSideTokenAfter == inputTokenValue);

    String methodStr1 = "depositTRX()";
    byte[] input1 = Hex.decode(AbiUtil.parseMethod(methodStr1, "", false));

    long callValue1 = 1500000000;
    String txid1 = PublicMethed
        .triggerContract(WalletClient.decodeFromBase58Check(mainGateWayAddress),
            callValue1,
            input1,
            maxFeeLimit, 0, "", depositAddress, testKeyFordeposit, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    //withdrawTrc10
    String withdrawToken = Long.toString(inputTokenValue - 1);
    String txid2 = PublicMethed
        .withdrawTrc10(inputTokenID, withdrawToken, ChainIdAddress,
            sideGatewayAddress,
            0,
            maxFeeLimit, depositAddress, testKeyFordeposit, blockingStubFull, blockingSideStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingSideStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingSideStubFull);
    Optional<TransactionInfo> infoById2 = PublicMethed
        .getTransactionInfoById(txid2, blockingStubFull);
    Assert.assertTrue(infoById2.get().getResultValue() == 0);
    long fee2 = infoById2.get().getFee();
    logger.info("fee2:" + fee2);
    Account accountWithdrawSideAfter = PublicMethed
        .queryAccount(depositAddress, blockingSideStubFull);
    long accountWithdrawSideAfterBalance = accountWithdrawSideAfter.getBalance();
    ByteString addressWithdrawSideAfter = accountWithdrawSideAfter.getAddress();
    String addressWithdrawSideAfterAddress = Base58
        .encode58Check(addressWithdrawSideAfter.toByteArray());
    logger.info("addressWithdrawSideAfterAddress:" + addressWithdrawSideAfterAddress);
    Assert.assertEquals(Base58.encode58Check(depositAddress), addressWithdrawSideAfterAddress);
    Assert.assertEquals(callValue1, accountWithdrawSideAfterBalance);
    Long withdrawSideTokenAfter = PublicMethed
        .getAssetIssueValue(depositAddress, assetAccountId, blockingSideStubFull);

    long withdrawMainTokenAfter = PublicMethed
        .getAssetIssueValue(depositAddress, assetAccountId, blockingStubFull);
    logger.info("withdrawSideTokenAfter:" + withdrawSideTokenAfter);
    logger.info("withdrawMainTokenAfter:" + withdrawMainTokenAfter);
    Assert.assertTrue(1 == withdrawSideTokenAfter);
    Assert.assertTrue(depositMainTokenAfter + inputTokenValue - 1 == withdrawMainTokenAfter);

    //address is not exist

    ECKey ecKey2 = new ECKey(Utils.getRandom());
    byte[] withdrawAddress = ecKey2.getAddress();
    String withdrawAddressKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
    Assert.assertTrue(PublicMethed
        .sendcoin(withdrawAddress, 100000000L, testDepositAddress, testDepositTrx,
            blockingStubFull));
    Return response = PublicMethed
        .withdrawTrcForReturn(inputTokenID, withdrawToken, ChainIdAddress,
            sideGatewayAddress,
            0,
            maxFeeLimit, withdrawAddress, withdrawAddressKey, blockingStubFull,
            blockingSideStubFull);

    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals(
        "contract validate error : callerAddress account does not exist",
        response.getMessage().toStringUtf8());

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingSideStubFull);

    Account accountWithdrawSideAfter1 = PublicMethed
        .queryAccount(depositAddress, blockingSideStubFull);
    long accountWithdrawSideAfterBalance1 = accountWithdrawSideAfter1.getBalance();
    ByteString addressWithdrawSideAfter1 = accountWithdrawSideAfter1.getAddress();
    String addressWithdrawSideAfterAddress1 = Base58
        .encode58Check(addressWithdrawSideAfter1.toByteArray());
    logger.info("addressWithdrawSideAfterAddress:" + addressWithdrawSideAfterAddress1);
    Assert.assertEquals(Base58.encode58Check(depositAddress), addressWithdrawSideAfterAddress1);
    Assert.assertEquals(callValue1, accountWithdrawSideAfterBalance);
    Long withdrawSideTokenAfter1 = PublicMethed
        .getAssetIssueValue(depositAddress, assetAccountId, blockingSideStubFull);

    long withdrawMainTokenAfter1 = PublicMethed
        .getAssetIssueValue(depositAddress, assetAccountId, blockingStubFull);
    logger.info("withdrawSideTokenAfter:" + withdrawSideTokenAfter1);
    logger.info("withdrawMainTokenAfter:" + withdrawMainTokenAfter1);
    Assert.assertTrue(withdrawSideTokenAfter == withdrawSideTokenAfter1);
    Assert.assertTrue(withdrawMainTokenAfter == withdrawMainTokenAfter1);
  }


  @Test(enabled = true, description = "Withdraw Trc10 with value exception.")
  public void test2WithdrawTrc10002() {

    assetAccountId = PublicMethed
        .queryAccount(depositAddress, blockingStubFull).getAssetIssuedID();
    logger.info("The token ID: " + assetAccountId.toStringUtf8());
    Long depositMainTokenBefore = PublicMethed
        .getAssetIssueValue(depositAddress, assetAccountId, blockingStubFull);
    Long depositSideTokenBefore = PublicMethed
        .getAssetIssueValue(depositAddress, assetAccountId, blockingSideStubFull);

    Assert.assertTrue(depositSideTokenBefore == 1);

    Account accountMainBefore = PublicMethed.queryAccount(depositAddress, blockingStubFull);
    long accountMainBalance = accountMainBefore.getBalance();

    Account accountSideBefore = PublicMethed.queryAccount(depositAddress, blockingSideStubFull);
    long accountSideBeforeBalance = accountSideBefore.getBalance();
    logger.info("accountSideBeforeBalance:" + accountSideBeforeBalance);
    logger.info("depositMainTokenBefore:" + depositMainTokenBefore);
    logger.info("depositSideAddressTokenBefore:" + depositSideTokenBefore);
    //balance<value

    String withdrawToken = Long.toString(depositSideTokenBefore + 1);
    String inputTokenID = assetAccountId.toStringUtf8();

    Return response = PublicMethed
        .withdrawTrcForReturn(inputTokenID, withdrawToken, ChainIdAddress,
            sideGatewayAddress,
            0,
            maxFeeLimit, depositAddress, testKeyFordeposit, blockingStubFull, blockingSideStubFull);
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals(
        "contract validate error : assetBalance is not sufficient.",
        response.getMessage().toStringUtf8());

    //value  is -1
    String withdrawToken1 = Long.toString(-1);

    Return response1 = PublicMethed
        .withdrawTrcForReturn(inputTokenID, withdrawToken1, ChainIdAddress,
            sideGatewayAddress,
            0,
            maxFeeLimit, depositAddress, testKeyFordeposit, blockingStubFull, blockingSideStubFull);
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response1.getCode());
    Assert.assertEquals(
        "contract validate error : tokenValue must >= 0",
        response1.getMessage().toStringUtf8());

    // value is Long.MAX_VALUE+1
    String withdrawToken2 = Long.toString(Long.MAX_VALUE + 1);
    Return response2 = PublicMethed
        .withdrawTrcForReturn(inputTokenID, withdrawToken2, ChainIdAddress,
            sideGatewayAddress,
            0,
            maxFeeLimit, depositAddress, testKeyFordeposit, blockingStubFull, blockingSideStubFull);
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response2.getCode());
    Assert.assertEquals(
        "contract validate error : tokenValue must >= 0",
        response2.getMessage().toStringUtf8());

    // value is Long.MIN_VALUE - 1
    String withdrawToken3 = Long.toString(Long.MIN_VALUE - 1);
    Return response3 = PublicMethed
        .withdrawTrcForReturn(inputTokenID, withdrawToken3, ChainIdAddress,
            sideGatewayAddress,
            0,
            maxFeeLimit, depositAddress, testKeyFordeposit, blockingStubFull, blockingSideStubFull);
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response3.getCode());
    Assert.assertEquals(
        "contract validate error : assetBalance is not sufficient.",
        response3.getMessage().toStringUtf8());

    // value is 0
    long callValue4 = 0;
    String withdrawToken4 = Long.toString(0);
    String txid = PublicMethed
        .withdrawTrc10(inputTokenID, withdrawToken4, ChainIdAddress,
            sideGatewayAddress,
            0,
            maxFeeLimit, depositAddress, testKeyFordeposit, blockingStubFull, blockingSideStubFull);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingSideStubFull);
    Assert.assertTrue(infoById.get().getResultValue() != 0);
    Assert.assertEquals(FAILED, infoById.get().getResult());
    Assert.assertEquals("REVERT opcode executed",
        infoById.get().getResMessage().toStringUtf8());
    long fee = infoById.get().getFee();
    logger.info("fee:" + fee);

    Account accountWithdrawSideAfter = PublicMethed
        .queryAccount(depositAddress, blockingSideStubFull);
    long accountWithdrawSideAfterBalance = accountWithdrawSideAfter.getBalance();
    ByteString addressWithdrawSideAfter = accountWithdrawSideAfter.getAddress();
    String addressWithdrawSideAfterAddress = Base58
        .encode58Check(addressWithdrawSideAfter.toByteArray());
    logger.info("addressWithdrawSideAfterAddress:" + addressWithdrawSideAfterAddress);
    Assert.assertEquals(Base58.encode58Check(depositAddress), addressWithdrawSideAfterAddress);
    Assert.assertEquals(accountSideBeforeBalance - fee, accountWithdrawSideAfterBalance);
    Long withdrawSideTokenAfter = PublicMethed
        .getAssetIssueValue(depositAddress, assetAccountId, blockingSideStubFull);

    long withdrawMainTokenAfter = PublicMethed
        .getAssetIssueValue(depositAddress, assetAccountId, blockingStubFull);
    logger.info("withdrawSideTokenAfter:" + withdrawSideTokenAfter);
    logger.info("withdrawMainTokenAfter:" + withdrawMainTokenAfter);
    Assert.assertTrue(depositSideTokenBefore == withdrawSideTokenAfter);
    Assert.assertTrue(depositMainTokenBefore == withdrawMainTokenAfter);
  }


  @Test(enabled = true, description = "Withdraw Trc10 with tokenID exception.")
  public void test3WithdrawTrc10003() {

    Account accountMainBefore = PublicMethed.queryAccount(depositAddress, blockingStubFull);
    long accountMainBalance = accountMainBefore.getBalance();
    assetAccountId = PublicMethed
        .queryAccount(depositAddress, blockingStubFull).getAssetIssuedID();
    logger.info("The token ID: " + assetAccountId.toStringUtf8());
    Long depositMainTokenBefore = PublicMethed
        .getAssetIssueValue(depositAddress, assetAccountId, blockingStubFull);
    Long depositSideTokenBefore = PublicMethed
        .getAssetIssueValue(depositAddress, assetAccountId, blockingSideStubFull);

    Account accountSideBefore = PublicMethed.queryAccount(depositAddress, blockingSideStubFull);
    long accountSideBeforeBalance = accountSideBefore.getBalance();

    //system not exist tokenID

    String withdrawToken = Long.toString(depositSideTokenBefore + 1);
    String fakeTokenId = Long.toString(Long.valueOf(assetAccountId.toStringUtf8()) + 100);
    Return response = PublicMethed
        .withdrawTrcForReturn(fakeTokenId, withdrawToken, ChainIdAddress,
            sideGatewayAddress,
            0,
            maxFeeLimit, depositAddress, testKeyFordeposit, blockingStubFull, blockingSideStubFull);
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals(
        "contract validate error : No asset !",
        response.getMessage().toStringUtf8());

    //account not have this tokenID

    ByteString assetAccountId1 = PublicMethed
        .queryAccount(depositAddress2, blockingStubFull).getAssetIssuedID();
    String inputTokenID1 = assetAccountId1.toStringUtf8();

    String withdrawToken1 = Long.toString(1);
    String txid2 = PublicMethed
        .withdrawTrc10(inputTokenID1, withdrawToken1, ChainIdAddress,
            sideGatewayAddress,
            0,
            maxFeeLimit, depositAddress2, testKeyFordeposit2, blockingStubFull,
            blockingSideStubFull);

    logger.info("The token ID: " + assetAccountId1.toStringUtf8());
    Return response1 = PublicMethed
        .withdrawTrcForReturn(inputTokenID1, withdrawToken1, ChainIdAddress,
            sideGatewayAddress,
            0,
            maxFeeLimit, depositAddress, testKeyFordeposit, blockingStubFull, blockingSideStubFull);

    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response1.getCode());
    Assert.assertEquals(
        "contract validate error : No asset !",
        response1.getMessage().toStringUtf8());

    // tokenId is null
//    String fakeTokenId1 = "null";
//    Return response2 = PublicMethed
//        .withdrawTrcForReturn(fakeTokenId1, withdrawToken, mainGateWayAddress,
//            sideGatewayAddress,
//            0,
//            maxFeeLimit, depositAddress, testKeyFordeposit, blockingStubFull, blockingSideStubFull);
//    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response2.getCode());
//    Assert.assertEquals(
//        "contract validate error : tokenValue must >= 0",
//        response2.getMessage().toStringUtf8());

    // tokenId is 1000000
    String fakeTokenId2 = "1000000";
    Return response3 = PublicMethed
        .withdrawTrcForReturn(fakeTokenId2, withdrawToken, ChainIdAddress,
            sideGatewayAddress,
            0,
            maxFeeLimit, depositAddress, testKeyFordeposit, blockingStubFull, blockingSideStubFull);
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response3.getCode());
    Assert.assertEquals(
        "contract validate error : tokenId must > 1000000",
        response3.getMessage().toStringUtf8());

    // tokenID is Long.MAX_VALUE + 1
    String fakeTokenId3 = Long.toString(Long.MAX_VALUE + 1);
    Return response4 = PublicMethed
        .withdrawTrcForReturn(fakeTokenId3, withdrawToken, ChainIdAddress,
            sideGatewayAddress,
            0,
            maxFeeLimit, depositAddress, testKeyFordeposit, blockingStubFull, blockingSideStubFull);

    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response4.getCode());
    Assert.assertEquals(
        "contract validate error : tokenId must > 1000000",
        response4.getMessage().toStringUtf8());

    // tokenID is Long.MIN_VALUE - 1
    String fakeTokenId4 = Long.toString(Long.MIN_VALUE - 1);
    Return response5 = PublicMethed
        .withdrawTrcForReturn(fakeTokenId4, withdrawToken, ChainIdAddress,
            sideGatewayAddress,
            0,
            maxFeeLimit, depositAddress, testKeyFordeposit, blockingStubFull, blockingSideStubFull);

    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response5.getCode());
    Assert.assertEquals(
        "contract validate error : No asset !",
        response5.getMessage().toStringUtf8());
    Account accountWithdrawSideAfter = PublicMethed
        .queryAccount(depositAddress, blockingSideStubFull);
    long accountWithdrawSideAfterBalance = accountWithdrawSideAfter.getBalance();
    ByteString addressWithdrawSideAfter = accountWithdrawSideAfter.getAddress();
    String addressWithdrawSideAfterAddress = Base58
        .encode58Check(addressWithdrawSideAfter.toByteArray());
    logger.info("addressWithdrawSideAfterAddress:" + addressWithdrawSideAfterAddress);
    Assert.assertEquals(Base58.encode58Check(depositAddress), addressWithdrawSideAfterAddress);
    Long withdrawSideTokenAfter = PublicMethed
        .getAssetIssueValue(depositAddress, assetAccountId, blockingSideStubFull);

    long withdrawMainTokenAfter = PublicMethed
        .getAssetIssueValue(depositAddress, assetAccountId, blockingStubFull);
    logger.info("withdrawSideTokenAfter:" + withdrawSideTokenAfter);
    logger.info("withdrawMainTokenAfter:" + withdrawMainTokenAfter);
    Assert.assertTrue(depositSideTokenBefore == withdrawSideTokenAfter);
    Assert.assertTrue(depositMainTokenBefore == withdrawMainTokenAfter);
  }


  @Test(enabled = true, description = "Withdraw Trc10 with feelimit exception")
  public void test4WithdrawTrc10004() {

    Account accountMainBefore = PublicMethed.queryAccount(depositAddress, blockingStubFull);
    long accountMainBalance = accountMainBefore.getBalance();
    assetAccountId = PublicMethed
        .queryAccount(depositAddress, blockingStubFull).getAssetIssuedID();
    logger.info("The token ID: " + assetAccountId.toStringUtf8());
    Long depositMainTokenBefore = PublicMethed
        .getAssetIssueValue(depositAddress, assetAccountId, blockingStubFull);
    Long depositSideTokenBefore = PublicMethed
        .getAssetIssueValue(depositAddress, assetAccountId, blockingSideStubFull);

    Account accountSideBefore = PublicMethed.queryAccount(depositAddress, blockingSideStubFull);
    long accountSideBeforeBalance = accountSideBefore.getBalance();

    logger.info("accountBeforeBalance:" + accountMainBalance);
    logger.info("accountSideBeforeBalance:" + accountSideBeforeBalance);
    logger.info("depositMainTokenBefore:" + depositMainTokenBefore);
    logger.info("depositSideAddressTokenBefore:" + depositSideTokenBefore);

    String inputTokenID = assetAccountId.toStringUtf8();
    long inputTokenValue = 1;
    //withdrawTrc10
    String withdrawToken = Long.toString(inputTokenValue);
    String txid1 = PublicMethed
        .withdrawTrc10(inputTokenID, withdrawToken, ChainIdAddress,
            sideGatewayAddress,
            0,
            1, depositAddress, testKeyFordeposit, blockingStubFull, blockingSideStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingSideStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingSideStubFull);
    Optional<TransactionInfo> infoById1 = PublicMethed
        .getTransactionInfoById(txid1, blockingSideStubFull);
    Assert.assertTrue(infoById1.get().getResultValue() != 0);
    long fee1 = infoById1.get().getFee();
    logger.info("fee1:" + fee1);
    Account accountWithdrawSideAfter = PublicMethed
        .queryAccount(depositAddress, blockingSideStubFull);
    long accountWithdrawSideAfterBalance = accountWithdrawSideAfter.getBalance();
    ByteString addressWithdrawSideAfter = accountWithdrawSideAfter.getAddress();
    String addressWithdrawSideAfterAddress = Base58
        .encode58Check(addressWithdrawSideAfter.toByteArray());
    logger.info("addressWithdrawSideAfterAddress:" + addressWithdrawSideAfterAddress);
    Assert.assertEquals(Base58.encode58Check(depositAddress), addressWithdrawSideAfterAddress);
    Long withdrawSideTokenAfter = PublicMethed
        .getAssetIssueValue(depositAddress, assetAccountId, blockingSideStubFull);

    long withdrawMainTokenAfter = PublicMethed
        .getAssetIssueValue(depositAddress, assetAccountId, blockingStubFull);
    logger.info("withdrawSideTokenAfter:" + withdrawSideTokenAfter);
    logger.info("withdrawMainTokenAfter:" + withdrawMainTokenAfter);
    Assert.assertTrue(depositSideTokenBefore == withdrawSideTokenAfter);
    Assert.assertTrue(depositMainTokenBefore == withdrawMainTokenAfter);
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

}
