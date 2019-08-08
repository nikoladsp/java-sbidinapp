import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import java.net.HttpURLConnection;
import java.security.MessageDigest;
import java.security.SecureRandom;

class Utils {

    public static String codeVerifier() {
        SecureRandom sr = new SecureRandom();
        byte[] code = new byte[40];
        sr.nextBytes(code);

        return new String(Base64.encodeBase64URLSafe(code));
    }

    public static String codeChallenge(final String verifier) throws Exception {
        final byte[] bytes = verifier.getBytes("US-ASCII");
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(bytes, 0, bytes.length);
        final byte[] digest = md.digest();
        //Use Apache "Commons Codec" dependency. Import the Base64 class
        //import org.apache.commons.codec.binary.Base64;
        return Base64.encodeBase64URLSafeString(digest);
    }

    public static String getStateString(int n)
    {
        // chose a Character random from this String
        final String Alphabet = "ABCDEF0123456789";

        // create StringBuffer size of AlphaNumericString
        StringBuilder sb = new StringBuilder(n);

        for (int i = 0; i < n; ++i) {

            // generate a random number between
            // 0 to AlphaNumericString variable length
            int index = (int)(Alphabet.length() * Math.random());

            // add Character one by one in end of sb
            sb.append(Alphabet.charAt(index));
        }

        return sb.toString();
    }

    public static String getFinalURL(final HttpClient client, final String url) throws Exception {
        final HttpGet request = new HttpGet(url);

        final HttpResponse res = client.execute(request);

        final int status = res.getStatusLine().getStatusCode();

        if (HttpURLConnection.HTTP_MOVED_PERM == status ||  HttpURLConnection.HTTP_MOVED_TEMP == status) {
            final String redirectUrl = res.getFirstHeader("Location").getValue();

            return getFinalURL(client, redirectUrl);
        }
        return url;
    }
}
