package aws.sample.paymentcryptography.hmac;

import org.springframework.stereotype.Component;

import com.amazonaws.services.paymentcryptography.model.Alias;
import com.amazonaws.services.paymentcryptography.model.CreateKeyRequest;
import com.amazonaws.services.paymentcryptography.model.Key;
import com.amazonaws.services.paymentcryptography.model.KeyAlgorithm;
import com.amazonaws.services.paymentcryptography.model.KeyAttributes;
import com.amazonaws.services.paymentcryptography.model.KeyClass;
import com.amazonaws.services.paymentcryptography.model.KeyModesOfUse;
import com.amazonaws.services.paymentcryptography.model.KeyUsage;
import com.amazonaws.services.paymentcryptographydata.model.GenerateMacRequest;
import com.amazonaws.services.paymentcryptographydata.model.GenerateMacResult;
import com.amazonaws.services.paymentcryptographydata.model.MacAlgorithm;
import com.amazonaws.services.paymentcryptographydata.model.MacAttributes;
import com.amazonaws.services.paymentcryptographydata.model.VerifyMacRequest;
import com.amazonaws.services.paymentcryptographydata.model.VerifyMacResult;
import com.amazonaws.util.StringUtils;

import aws.sample.paymentcryptography.ControlPlaneUtils;
import aws.sample.paymentcryptography.DataPlaneUtils;
import aws.sample.paymentcryptography.ServiceConstants;

@Component
public class HMACService {

    private static final String HMAC_KEY_ALIAS = "alias/tr34-hmac-key-import";

    public String createHMACKey() {
        Alias hmacKeyAlias = ControlPlaneUtils.getOrCreateAlias(HMAC_KEY_ALIAS);

        if (!StringUtils.isNullOrEmpty(hmacKeyAlias.getKeyArn())) {
            return hmacKeyAlias.getKeyArn();
        }

        KeyModesOfUse modes = new KeyModesOfUse()
                .withGenerate(true)
                .withVerify(true);
        KeyAttributes attributes = new KeyAttributes()
                .withKeyAlgorithm(KeyAlgorithm.TDES_2KEY)
                .withKeyClass(KeyClass.SYMMETRIC_KEY)
                .withKeyUsage(KeyUsage.TR31_M3_ISO_9797_3_MAC_KEY)
                .withKeyModesOfUse(modes);
        CreateKeyRequest request = new CreateKeyRequest()
                .withKeyAttributes(attributes)
                .withEnabled(true)
                .withExportable(false);

        Key key = ControlPlaneUtils.getControlPlaneClient().createKey(request).getKey();
        ControlPlaneUtils.upsertAlias(hmacKeyAlias.getAliasName(), key.getKeyArn());
        return hmacKeyAlias.getAliasName();
    }

    public String generateMac() {
        String hmacKeyArn = createHMACKey();
        GenerateMacResult macGenerateResult = generateMac(hmacKeyArn);
        return macGenerateResult.getMac();
    }

    public GenerateMacResult generateMac(String hmacKeyArn) {
        MacAttributes macAttributes = new MacAttributes()
                .withAlgorithm(MacAlgorithm.ISO9797_ALGORITHM3);
        GenerateMacRequest generateMacRequest = new GenerateMacRequest()
                .withKeyIdentifier(hmacKeyArn)
                .withMessageData(ServiceConstants.HMAC_DATA_PLAIN_TEXT)
                .withGenerationAttributes(macAttributes);
        GenerateMacResult macGenerateResult = DataPlaneUtils.getDataPlaneClient().generateMac(generateMacRequest);
        return macGenerateResult;
    }

    public VerifyMacResult getMacVerification(String hmacKeyArn, String mac) {
        MacAttributes macAttributes = new MacAttributes()
                .withAlgorithm(MacAlgorithm.ISO9797_ALGORITHM3);
        VerifyMacRequest verifyMacRequest = new VerifyMacRequest()
                .withKeyIdentifier(hmacKeyArn)
                .withVerificationAttributes(macAttributes)
                .withMac(mac)
                .withMessageData(ServiceConstants.HMAC_DATA_PLAIN_TEXT);
        VerifyMacResult macVerificationResult = DataPlaneUtils.getDataPlaneClient().verifyMac(verifyMacRequest);
        return macVerificationResult;
    }
}
