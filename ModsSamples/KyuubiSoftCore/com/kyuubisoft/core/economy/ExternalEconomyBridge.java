/*
 * Decompiled with CFR 0.152.
 */
package com.kyuubisoft.core.economy;

import com.kyuubisoft.core.api.CoreAPI;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.logging.Logger;

public class ExternalEconomyBridge {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft Core");
    private static final ExternalEconomyBridge instance = new ExternalEconomyBridge();
    private static final String PLUGIN_NAME = "KyuubiSoft";
    private Backend activeBackend = Backend.NONE;
    private boolean initialized = false;
    private Method isEnabledMethod;
    private Method getBalanceMethod;
    private Method depositMethod;
    private Method withdrawMethod;
    private Method hasMethod;
    private Method formatMethod;
    private Method getCurrencyNameMethod;
    private Object vaultEconomyInstance;
    private Method vaultIsEnabledMethod;
    private Method vaultGetBalanceMethod;
    private Method vaultDepositMethod;
    private Method vaultWithdrawMethod;
    private Method vaultHasMethod;
    private Method vaultFormatMethod;
    private Method vaultCurrencyNameMethod;
    private Method vaultGetNameMethod;
    private String vaultProviderName;

    private ExternalEconomyBridge() {
    }

    public static ExternalEconomyBridge getInstance() {
        return instance;
    }

    public boolean initialize(String preferredProvider) {
        if (this.initialized) {
            return this.isAvailable();
        }
        this.initialized = true;
        if ("internal".equals(preferredProvider) || "command".equals(preferredProvider)) {
            LOGGER.info("Economy provider set to '" + preferredProvider + "', skipping external detection");
            return false;
        }
        if (("auto".equals(preferredProvider) || "vaultunlocked".equals(preferredProvider)) && this.tryVaultUnlocked()) {
            LOGGER.info("External economy detected: VAULT_UNLOCKED (" + this.vaultProviderName + ")");
            return true;
        }
        if (("auto".equals(preferredProvider) || "eliteessentials".equals(preferredProvider)) && this.tryEliteEssentials()) {
            LOGGER.info("External economy detected: ELITE_ESSENTIALS");
            return true;
        }
        LOGGER.info("No external economy detected, using command fallback");
        return false;
    }

    private boolean tryVaultUnlocked() {
        try {
            Class<?> vaultClass = Class.forName("net.cfh.vault.VaultUnlocked");
            Method economyObjMethod = vaultClass.getMethod("economyObj", new Class[0]);
            Object economy = economyObjMethod.invoke(null, new Object[0]);
            if (economy == null) {
                LOGGER.info("VaultUnlocked found but no economy backend registered");
                return false;
            }
            Class<?> economyClass = economy.getClass();
            if (CoreAPI.isDebug()) {
                LOGGER.info("VaultUnlocked economy class: " + economyClass.getName());
            }
            this.vaultIsEnabledMethod = this.tryResolveMethod(economyClass, "isEnabled", new Class[0]);
            if (this.vaultIsEnabledMethod != null && !((Boolean)this.vaultIsEnabledMethod.invoke(economy, new Object[0])).booleanValue()) {
                LOGGER.info("VaultUnlocked economy backend is disabled");
                return false;
            }
            this.vaultGetBalanceMethod = this.tryResolveMethod(economyClass, "getBalance", String.class, UUID.class);
            this.vaultDepositMethod = this.tryResolveMethod(economyClass, "deposit", String.class, UUID.class, BigDecimal.class);
            this.vaultWithdrawMethod = this.tryResolveMethod(economyClass, "withdraw", String.class, UUID.class, BigDecimal.class);
            this.vaultHasMethod = this.tryResolveMethod(economyClass, "has", String.class, UUID.class, BigDecimal.class);
            this.vaultFormatMethod = this.tryResolveMethod(economyClass, "format", BigDecimal.class);
            this.vaultCurrencyNameMethod = this.tryResolveMethod(economyClass, "defaultCurrencyNamePlural", String.class);
            if (this.vaultCurrencyNameMethod == null) {
                this.vaultCurrencyNameMethod = this.tryResolveMethod(economyClass, "currencyNamePlural", new Class[0]);
            }
            this.vaultGetNameMethod = this.tryResolveMethod(economyClass, "getName", new Class[0]);
            if (this.vaultGetBalanceMethod == null || this.vaultDepositMethod == null || this.vaultWithdrawMethod == null) {
                LOGGER.warning("VaultUnlocked economy backend missing required methods. Found: getBalance=" + (this.vaultGetBalanceMethod != null) + ", deposit=" + (this.vaultDepositMethod != null) + ", withdraw=" + (this.vaultWithdrawMethod != null));
                for (Method m : economyClass.getMethods()) {
                    if (!m.getName().contains("Balance") && !m.getName().contains("deposit") && !m.getName().contains("withdraw") && !m.getName().contains("Deposit") && !m.getName().contains("Withdraw")) continue;
                    StringBuilder params = new StringBuilder();
                    for (Class<?> p : m.getParameterTypes()) {
                        if (params.length() > 0) {
                            params.append(", ");
                        }
                        params.append(p.getSimpleName());
                    }
                    LOGGER.warning("  Available: " + m.getName() + "(" + String.valueOf(params) + ") \u2192 " + m.getReturnType().getSimpleName());
                }
                return false;
            }
            this.vaultEconomyInstance = economy;
            if (this.vaultGetNameMethod != null) {
                try {
                    this.vaultProviderName = (String)this.vaultGetNameMethod.invoke(economy, new Object[0]);
                }
                catch (Exception e) {
                    this.vaultProviderName = "Unknown";
                }
            } else {
                this.vaultProviderName = economyClass.getSimpleName();
            }
            this.activeBackend = Backend.VAULT_UNLOCKED;
            return true;
        }
        catch (ClassNotFoundException e) {
            return false;
        }
        catch (Exception e) {
            LOGGER.warning("VaultUnlocked economy init failed: " + e.getMessage());
            return false;
        }
    }

    private boolean tryEliteEssentials() {
        try {
            Class<?> api = Class.forName("com.eliteessentials.api.EconomyAPI");
            this.isEnabledMethod = api.getMethod("isEnabled", new Class[0]);
            if (!((Boolean)this.isEnabledMethod.invoke(null, new Object[0])).booleanValue()) {
                return false;
            }
            this.getBalanceMethod = api.getMethod("getBalance", UUID.class);
            this.depositMethod = api.getMethod("deposit", UUID.class, Double.TYPE);
            this.withdrawMethod = api.getMethod("withdraw", UUID.class, Double.TYPE);
            this.hasMethod = api.getMethod("has", UUID.class, Double.TYPE);
            this.formatMethod = api.getMethod("format", Double.TYPE);
            this.getCurrencyNameMethod = api.getMethod("getCurrencyName", new Class[0]);
            this.activeBackend = Backend.ELITE_ESSENTIALS;
            return true;
        }
        catch (ClassNotFoundException e) {
            return false;
        }
        catch (Exception e) {
            LOGGER.warning("EliteEssentials economy init failed: " + e.getMessage());
            return false;
        }
    }

    private Method tryResolveMethod(Class<?> clazz, String name, Class<?> ... paramTypes) {
        try {
            return clazz.getMethod(name, paramTypes);
        }
        catch (NoSuchMethodException e) {
            return null;
        }
    }

    private boolean extractTransactionSuccess(Object response) {
        try {
            Method successMethod = response.getClass().getMethod("transactionSuccess", new Class[0]);
            return (Boolean)successMethod.invoke(response, new Object[0]);
        }
        catch (Exception e) {
            if (response instanceof Boolean) {
                Boolean b = (Boolean)response;
                return b;
            }
            return false;
        }
    }

    public double getBalance(UUID playerId) {
        if (!this.checkEnabled()) {
            return 0.0;
        }
        try {
            if (this.activeBackend == Backend.VAULT_UNLOCKED) {
                BigDecimal result = (BigDecimal)this.vaultGetBalanceMethod.invoke(this.vaultEconomyInstance, PLUGIN_NAME, playerId);
                return result != null ? result.doubleValue() : 0.0;
            }
            return (Double)this.getBalanceMethod.invoke(null, playerId);
        }
        catch (Exception e) {
            this.handleError("getBalance", e);
            return 0.0;
        }
    }

    public boolean deposit(UUID playerId, double amount) {
        if (!this.checkEnabled()) {
            return false;
        }
        try {
            if (this.activeBackend == Backend.VAULT_UNLOCKED) {
                Object response = this.vaultDepositMethod.invoke(this.vaultEconomyInstance, PLUGIN_NAME, playerId, BigDecimal.valueOf(amount));
                return this.extractTransactionSuccess(response);
            }
            return (Boolean)this.depositMethod.invoke(null, playerId, amount);
        }
        catch (Exception e) {
            this.handleError("deposit", e);
            return false;
        }
    }

    public boolean withdraw(UUID playerId, double amount) {
        if (!this.checkEnabled()) {
            return false;
        }
        try {
            if (this.activeBackend == Backend.VAULT_UNLOCKED) {
                Object response = this.vaultWithdrawMethod.invoke(this.vaultEconomyInstance, PLUGIN_NAME, playerId, BigDecimal.valueOf(amount));
                return this.extractTransactionSuccess(response);
            }
            return (Boolean)this.withdrawMethod.invoke(null, playerId, amount);
        }
        catch (Exception e) {
            this.handleError("withdraw", e);
            return false;
        }
    }

    public boolean has(UUID playerId, double amount) {
        if (!this.checkEnabled()) {
            return false;
        }
        try {
            if (this.activeBackend == Backend.VAULT_UNLOCKED) {
                if (this.vaultHasMethod != null) {
                    return (Boolean)this.vaultHasMethod.invoke(this.vaultEconomyInstance, PLUGIN_NAME, playerId, BigDecimal.valueOf(amount));
                }
                return this.getBalance(playerId) >= amount;
            }
            return (Boolean)this.hasMethod.invoke(null, playerId, amount);
        }
        catch (Exception e) {
            this.handleError("has", e);
            return false;
        }
    }

    public String format(double amount) {
        if (!this.checkEnabled()) {
            return String.valueOf((int)amount);
        }
        try {
            if (this.activeBackend == Backend.VAULT_UNLOCKED) {
                if (this.vaultFormatMethod != null) {
                    return (String)this.vaultFormatMethod.invoke(this.vaultEconomyInstance, BigDecimal.valueOf(amount));
                }
                return String.valueOf((int)amount);
            }
            return (String)this.formatMethod.invoke(null, amount);
        }
        catch (Exception e) {
            this.handleError("format", e);
            return String.valueOf((int)amount);
        }
    }

    public String getCurrencyName() {
        if (!this.checkEnabled()) {
            return null;
        }
        try {
            if (this.activeBackend == Backend.VAULT_UNLOCKED) {
                if (this.vaultCurrencyNameMethod != null) {
                    if (this.vaultCurrencyNameMethod.getParameterCount() == 1) {
                        return (String)this.vaultCurrencyNameMethod.invoke(this.vaultEconomyInstance, PLUGIN_NAME);
                    }
                    return (String)this.vaultCurrencyNameMethod.invoke(this.vaultEconomyInstance, new Object[0]);
                }
                return null;
            }
            return (String)this.getCurrencyNameMethod.invoke(null, new Object[0]);
        }
        catch (Exception e) {
            this.handleError("getCurrencyName", e);
            return null;
        }
    }

    public boolean isAvailable() {
        return this.activeBackend != Backend.NONE;
    }

    public Backend getActiveBackend() {
        return this.activeBackend;
    }

    public String getProviderName() {
        return switch (this.activeBackend.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> {
                if (this.vaultProviderName != null) {
                    yield this.vaultProviderName;
                }
                yield "VaultUnlocked";
            }
            case 1 -> "EliteEssentials";
            case 2 -> "None";
        };
    }

    private boolean checkEnabled() {
        if (this.activeBackend == Backend.NONE) {
            return false;
        }
        try {
            if (this.activeBackend == Backend.VAULT_UNLOCKED) {
                if (this.vaultIsEnabledMethod != null) {
                    return (Boolean)this.vaultIsEnabledMethod.invoke(this.vaultEconomyInstance, new Object[0]);
                }
                return true;
            }
            return (Boolean)this.isEnabledMethod.invoke(null, new Object[0]);
        }
        catch (Exception e) {
            LOGGER.warning("Economy backend no longer available: " + e.getMessage());
            this.activeBackend = Backend.NONE;
            return false;
        }
    }

    private void handleError(String method, Exception e) {
        LOGGER.warning("Economy " + method + " failed: " + e.getMessage());
    }

    public static enum Backend {
        VAULT_UNLOCKED,
        ELITE_ESSENTIALS,
        NONE;

    }
}

