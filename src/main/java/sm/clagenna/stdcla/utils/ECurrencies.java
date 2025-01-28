package sm.clagenna.stdcla.utils;

public enum ECurrencies {
  Afghani("Afghani", "\u060B"), //
  Armenian_Dram("Armenian Dram", "\u058F"), //
  Austral("Austral", "\u20B3"), //
  Bengali_Ganda_Mark("Bengali Ganda Mark", "\u09FB"), //
  Bengali_Rupee("Bengali Rupee", "\u09F3"), //
  Bengali_Rupee_Mark("Bengali Rupee Mark", "\u09F2"), //
  Bitcoin("Bitcoin", "\u20BF"), //
  Cedi("Cedi", "\u20B5"), //
  Cent("Cent", "\u00A2"), //
  Colon("Colon", "\u20A1"), //
  Cruzeiro("Cruzeiro", "\u20A2"), //
  Currency("Currency", "\u00A4"), //
  Dollar("Dollar", "\u0024"), //
  Dong("Dong", "\u20AB"), //
  Drachma("Drachma", "\u20AF"), //
  Euro("Euro", "\u20AC"), //
  Euro_Currency("Euro-Currency", "\u20A0"), //
  French_Franc("French Franc", "\u20A3"), //
  Fullwidth_Cent("Fullwidth Cent", "\uFFE0"), //
  Fullwidth_Dollar("Fullwidth Dollar", "\uFF04"), //
  Fullwidth_Pound("Fullwidth Pound", "\uFFE1"), //
  Fullwidth_Won("Fullwidth Won", "\uFFE6"), //
  Fullwidth_Yen("Fullwidth Yen", "\uFFE5"), //
  German_Penny("German Penny", "\u20B0"), //
  Guarani("Guarani", "\u20B2"), //
  Gujarati_Rupee("Gujarati Rupee", "\u0AF1"), //
  Hryvnia("Hryvnia", "\u20B4"), //
  Indian_Rupee("Indian Rupee", "\u20B9"), //
  Indic_Siyaq_Rupee_Mark("Indic Siyaq Rupee Mark", "\u1ECB0"), //
  Khmer_Currency_Symbol_Riel("Khmer Currency Symbol Riel", "\u17DB"), //
  Kip("Kip", "\u20AD"), //
  Lari("Lari", "\u20BE"), //
  Lira("Lira", "\u20A4"), //
  Livre_Tournois("Livre Tournois", "\u20B6"), //
  Manat("Manat", "\u20BC"), //
  Mill("Mill", "\u20A5"), //
  Naira("Naira", "\u20A6"), //
  New_Sheqel("New Sheqel", "\u20AA"), //
  Nko_Dorome("Nko Dorome", "\u07FE"), //
  Nko_Taman("Nko Taman", "\u07FF"), //
  Nordic_Mark("Nordic Mark", "\u20BB"), //
  North_Indic_Rupee_Mark("North Indic Rupee Mark", "\uA838"), //
  Peseta("Peseta", "\u20A7"), //
  Peso("Peso", "\u20B1"), //
  Pound("Pound", "\u00A3"), //
  Rial("Rial", "\uFDFC"), //
  Ruble("Ruble", "\u20BD"), //
  Rupee("Rupee", "\u20A8"), //
  Small_Dollar("Small Dollar", "\uFE69"), //
  Spesmilo("Spesmilo", "\u20B7"), //
  Tamil_Kaacu("Tamil Kaacu", "\u11FDD"), //
  Tamil_Panam("Tamil Panam", "\u11FDE"), //
  Tamil_Pon("Tamil Pon", "\u11FDF"), //
  Tamil_Rupee("Tamil Rupee", "\u0BF9"), //
  Tamil_Varaakan("Tamil Varaakan", "\u11FE0"), //
  Tenge("Tenge", "\u20B8"), //
  Thai_Currency_Symbol_Baht("Thai Currency Symbol Baht", "\u0E3F"), //
  Tugrik("Tugrik", "\u20AE"), //
  Turkish_Lira("Turkish Lira", "\u20BA"), //
  Wancho_Ngun("Wancho Ngun", "\u1E2FF"), //
  Won("Won", "\u20A9"), //
  Yen("Yen", "\u00A5");

  private String name;
  private String symbol;

  private ECurrencies(String nam, String sym) {
    name = nam;
    symbol = sym;
  }

  public String getName() {
    return name;
  }

  public String getSymbol() {
    return symbol;
  }
}
