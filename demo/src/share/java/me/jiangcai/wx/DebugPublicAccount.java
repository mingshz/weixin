package me.jiangcai.wx;

import me.jiangcai.wx.model.PublicAccount;

/**
 * @author CJ
 */
@SuppressWarnings("WeakerAccess")
public class DebugPublicAccount extends PublicAccount {

    private PublicAccountSupplier supplier;

    public DebugPublicAccount() {
        this("http://localhost/");
    }

    public DebugPublicAccount(String url) {
        setAppID("wx59b0162cdf0967af");
        setAppSecret("ffcf655fce7c4175bbddae7b594c4e27");
        setInterfaceURL(url);
        setInterfaceToken("jiangcai");
    }

    @Override
    public PublicAccountSupplier getSupplier() {
        return supplier;
    }

    public void setSupplier(PublicAccountSupplier supplier) {
        this.supplier = supplier;
    }
}
