import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SbidInapp {

    private static BasicCookieStore cookieStore = new BasicCookieStore();
    private static HttpClient client = HttpClientBuilder.create()
            .setDefaultCookieStore(cookieStore)
            .disableRedirectHandling().build();

    public static void main(String[] args) {
        try {
            SbidInapp app = new SbidInapp();

            final String verifier = Utils.codeVerifier();
            final HttpResponse res1 = app.Step1(verifier);
            final JSONObject res2 = app.Step2(res1);
            final String code = app.Step3(res2);
            final String accessToken = app.Step4(code, verifier);
            app.Step5(accessToken);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private HttpResponse Step1(final String verifier) throws Exception {
        // STEP 1: Call authorize using method "sbid-inapp"

        final String state = Utils.getStateString(8);
        final String nid = "197602208253";
        final String challenge = Utils.codeChallenge(verifier);

        final String url = String.format("https://preprod.signicat.com/oidc/authorize?response_type=code&scope=openid+profile+signicat.national_id+phone&client_id=demo-inapp&redirect_uri=https://example.com/redirect&acr_values=urn:signicat:oidc:method:sbid-inapp&state=%s&login_hint=subject-%s&code_challenge_method=S256&code_challenge=%s", state, nid, challenge);

        HttpGet request = new HttpGet(url);
        request.addHeader("accept", "application/json");
        request.addHeader("User-Agent", "python-requests/2.22.0");
        request.addHeader("Accept-Encoding", "gzip, deflate");
        request.addHeader("Connection", "keep-alive");

        return client.execute(request);
    }

    private JSONObject Step2(final HttpResponse response1) throws Exception {
        // STEP 2: Poll collectUrl until progressStatus=COMPLETE

        HttpResponse res = null;
        JSONObject data = null;

        final JSONObject jsonData = new JSONObject(EntityUtils.toString(response1.getEntity(), StandardCharsets.UTF_8));
        final String orderRef = (String) jsonData.get("orderRef");
        final String autoStartToken = (String) jsonData.get("autoStartToken");
        final String collectUrl = (String) jsonData.get("collectUrl");
        final String url = String.format("%s?orderRef=%s", collectUrl, orderRef);

        HttpGet request = new HttpGet(url);
        request.addHeader("accept", "application/json");

        String progressStatus = new String();
        final String finalStatus = "COMPLETE";
        System.out.println("\nPolling...");
        while (!finalStatus.equals(progressStatus)) { // Check if COMPLETE, if not sleep 5s and check again.
            Thread.sleep(5000);

            res = client.execute(request);
            data = new JSONObject(EntityUtils.toString(res.getEntity(), StandardCharsets.UTF_8));

            progressStatus = (String) data.get("progressStatus");

            System.out.println(String.format("  -- Status: %s", progressStatus));
        }

        System.out.println(String.format("collectUrl Response: %s", res));

        return data;
    }

    private String Step3(final JSONObject jsonData) throws Exception {
        // STEP 3: Call completeUrl - the last redirect will contain CODE and STATE.

        final String completeUrl = (String) jsonData.get("completeUrl");

        final String url = Utils.getFinalURL(client, completeUrl);

        Map<String, String> params = new HashMap<String, String>();
        for (NameValuePair param : URLEncodedUtils.parse(new URI(url), "UTF-8")) {
            params.put(param.getName(), param.getValue());
        }

        final String code = params.get("code");
        final String state = params.get("state");

        System.out.println(String.format("\nFinal redirect URL: %s", url));
        System.out.println(String.format("  -- CODE: '%s'", code));
        System.out.println(String.format("  -- STATE: '%s'", state));

        return code;
    }

    private String Step4(final String code, final String verifier) throws Exception {
        // STEP 4: Call /token end-point as normal (using CODE we got in STEP 3)

        List<NameValuePair> payload = new ArrayList<NameValuePair>();
        payload.add(new BasicNameValuePair("client_id", "demo-inapp"));
        payload.add(new BasicNameValuePair("redirect_uri", "https://example.com/redirect"));
        payload.add(new BasicNameValuePair("grant_type", "authorization_code"));
        payload.add(new BasicNameValuePair("code_verifier", verifier));
        payload.add(new BasicNameValuePair("code", code));

        HttpPost request = new HttpPost("https://preprod.signicat.com/oidc/token");
        request.setHeader("Authorization", "Basic ZGVtby1pbmFwcDptcVotXzc1LWYyd05zaVFUT05iN09uNGFBWjd6YzIxOG1yUlZrMW91ZmE4");
        request.setEntity(new UrlEncodedFormEntity(payload, "UTF-8"));

        final HttpResponse res = client.execute(request);

        final JSONObject jsonData = new JSONObject(EntityUtils.toString(res.getEntity(), StandardCharsets.UTF_8));
        final String accessToken = (String) jsonData.get("access_token");

        System.out.println(String.format("\nAccess Token: %s", accessToken));

        return accessToken;
    }

    private JSONObject Step5(final String accessToken) throws Exception {
        // STEP 5 (optional): Call /userinfo with access token.

        HttpGet request = new HttpGet("https://preprod.signicat.com/oidc/userinfo");
        request.setHeader("Authorization", String.format("Bearer %s", accessToken));

        final HttpResponse res = client.execute(request);

        final JSONObject userInfo = new JSONObject(EntityUtils.toString(res.getEntity(), StandardCharsets.UTF_8));

        System.out.println(String.format("UserInfo Response: %s", userInfo));

        return userInfo;
    }
}
