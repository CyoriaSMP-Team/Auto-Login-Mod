package cloud.sect.alm.cloud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import cloud.sect.alm.AutoLoginModClient;

public class IdentityManager {
    
    public enum AccountStatus {
        VERIFIED_MOJANG, // ไอดีแท้ (Microsoft)
        LOCAL_ACCOUNT,   // ไอดีเถื่อน/Local
        UNTRUSTED        // ไม่สามารถระบุได้
    }

    public static AccountStatus checkIdentity() {
        Minecraft mc = Minecraft.getInstance();
        User user = mc.getUser();
        
        // Minecraft 1.21.11 removed User.Type. An MSA XUID and nonempty
        // access token indicate a Microsoft launcher session, but this is
        // a client-side hint, NOT independent authentication verification.
        if (user != null
                && user.getXuid().filter(id -> !id.isBlank()).isPresent()
                && user.getAccessToken() != null
                && !user.getAccessToken().isBlank()) {
            AutoLoginModClient.LOGGER.info("[ALM-Cloud] Microsoft session markers present.");
            return AccountStatus.VERIFIED_MOJANG;
        }

        AutoLoginModClient.LOGGER.warn("§6[ALM-Cloud] §cUnverified Local Account detected. Cloud Sync Disabled.");
        return AccountStatus.LOCAL_ACCOUNT;
    }

    public static boolean canUseCloud() {
        // เงื่อนไข: ต้องเป็นไอดีแท้เท่านั้นถึงจะใช้ระบบเมฆได้
        return checkIdentity() == AccountStatus.VERIFIED_MOJANG;
    }
}
