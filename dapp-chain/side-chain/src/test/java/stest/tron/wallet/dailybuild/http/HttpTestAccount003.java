package stest.tron.wallet.dailybuild.http;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.HttpMethed;
import stest.tron.wallet.common.client.utils.PublicMethedForDailybuild;

@Slf4j
public class HttpTestAccount003 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethedForDailybuild.getFinalAddress(testKey002);
  private JSONObject responseContent;
  private HttpResponse response;
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(0);
  private String httpSoliditynode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(2);
  private final String witnessKey001 = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  private final byte[] witness1Address = PublicMethedForDailybuild.getFinalAddress(witnessKey001);
  private final String witnessKey002 = Configuration.getByPath("testng.conf")
      .getString("witness.key2");
  private final byte[] witness2Address = PublicMethedForDailybuild.getFinalAddress(witnessKey002);

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] newAccountAddress = ecKey1.getAddress();
  String newAccountKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());


  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] updateAccountAddress = ecKey2.getAddress();
  String updateAccountKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  private final Long createWitnessAmount = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.createWitnessAmount");
  Long amount = 50000000L;
  private static String updateAccountName = "updateAccount_"
      + Long.toString(System.currentTimeMillis());
  private static String updateUrl =
      "http://www.update.url" + Long.toString(System.currentTimeMillis());

  JsonArray voteKeys = new JsonArray();
  JsonObject voteElement = new JsonObject();


  /**
   * constructor.
   */
  @Test(enabled = true, description = "Update account by http")
  public void test1UpdateAccount() {
    response = HttpMethed.sendCoin(httpnode, fromAddress, updateAccountAddress, amount, testKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);

    response = HttpMethed.updateAccount(httpnode, updateAccountAddress, updateAccountName,
        updateAccountKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);

    response = HttpMethed.getAccount(httpnode, updateAccountAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.getString("account_name")
        .equalsIgnoreCase(HttpMethed.str2hex(updateAccountName)));

    Assert.assertFalse(responseContent.getString("active_permission").isEmpty());
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Vote witness account by http")
  public void test2VoteWitnessAccount() {
    //Freeze balance
    response = HttpMethed.freezeBalance(httpnode, updateAccountAddress, 40000000L, 0,
        0, updateAccountKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    voteElement.addProperty("vote_address", ByteArray.toHexString(witness1Address));
    voteElement.addProperty("vote_count", 11);
    voteKeys.add(voteElement);

    voteElement.remove("vote_address");
    voteElement.remove("vote_count");
    voteElement.addProperty("vote_address", ByteArray.toHexString(witness2Address));
    voteElement.addProperty("vote_count", 12);
    voteKeys.add(voteElement);

    response = HttpMethed.voteWitnessAccount(httpnode, updateAccountAddress, voteKeys,
        updateAccountKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.getAccount(httpnode, updateAccountAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(!responseContent.getString("votes").isEmpty());
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "List witnesses by http")
  public void test3ListWitness() {
    response = HttpMethed.listwitnesses(httpnode);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    JSONArray jsonArray = JSONArray.parseArray(responseContent.getString("witnesses"));
    Assert.assertTrue(jsonArray.size() >= 2);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "List witnesses from solidity by http")
  public void test4ListWitnessFromSolidity() {
    HttpMethed.waitToProduceOneBlockFromSolidity(httpnode, httpSoliditynode);
    response = HttpMethed.listwitnessesFromSolidity(httpSoliditynode);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    JSONArray jsonArray = JSONArray.parseArray(responseContent.getString("witnesses"));
    Assert.assertTrue(jsonArray.size() >= 2);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Update witness by http")
  public void test5UpdateWitness() {
    response = HttpMethed.updateWitness(httpnode, witness1Address, updateUrl, witnessKey001);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);

    response = HttpMethed.listwitnesses(httpnode);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.getString("witnesses").indexOf(updateUrl) != -1);
    //logger.info("result is " + responseContent.getString("witnesses").indexOf(updateUrl));
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Create account by http")
  public void test6CreateAccount() {
    PublicMethedForDailybuild.printAddress(newAccountKey);
    response = HttpMethed.createAccount(httpnode, fromAddress, newAccountAddress, testKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.getAccount(httpnode, newAccountAddress);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.getLong("create_time") > 3);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Create witness by http")
  public void test7CreateWitness() {
    response = HttpMethed.sendCoin(httpnode, fromAddress, newAccountAddress, createWitnessAmount,
        testKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    HttpMethed.waitToProduceOneBlock(httpnode);
    PublicMethedForDailybuild.printAddress(newAccountKey);

    response = HttpMethed.createWitness(httpnode, newAccountAddress, updateUrl);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(!responseContent.getString("txID").isEmpty());
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Withdraw by http")
  public void test8Withdraw() {
    response = HttpMethed.withdrawBalance(httpnode, witness2Address, witnessKey002);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.getString("result").equals("true"));
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethed.disConnect();
  }
}
