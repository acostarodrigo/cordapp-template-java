package org.shield;

import com.paypal.api.openidconnect.Session;
import com.paypal.api.payments.*;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class PayPalPaymentTest {
    private static String clientID = "AYSq3RDGsmBLJE-otTkBtM-jBRd1TCQwFf9RGfwddNXWz0uFU9ztymylOhRS";
    private static String clientSecret = "EGnHDxD_qRPdaLdZz8iCr8N7_MzF-YHPTkjs6NKYQvQSBngp4PTTVWkPZRbL";

    @Test
    public void main(){
        APIContext context = new APIContext(clientID, clientSecret, "sandbox");

        List<String> scopes = new ArrayList<String>() {{
            /**
             * 'openid'
             * 'profile'
             * 'address'
             * 'email'
             * 'phone'
             * 'https://uri.paypal.com/services/paypalattributes'
             * 'https://uri.paypal.com/services/expresscheckout'
             * 'https://uri.paypal.com/services/invoicing'
             */
            add("openid");
            add("profile");
            add("email");
        }};
        String redirectUrl = Session.getRedirectURL("UserConsent", scopes, context);
        System.out.println(redirectUrl);
    }
}
