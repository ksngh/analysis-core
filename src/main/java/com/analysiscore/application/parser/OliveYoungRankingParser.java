package com.analysiscore.application.parser;

import com.analysiscore.domain.entity.RankingItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OliveYoungRankingParser {
    private static final Logger log = LoggerFactory.getLogger(OliveYoungRankingParser.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<RankingItem> parse(String contentType, String body) {
        String trimmed = body == null ? "" : body.trim();
        boolean looksJson = (contentType != null && contentType.toLowerCase(Locale.ROOT).contains("json"))
            || trimmed.startsWith("{")
            || trimmed.startsWith("[");

        if (looksJson) {
            return parseFromJson(trimmed);
        }
        return parseFromHtml(trimmed);
    }

    private List<RankingItem> parseFromJson(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root.isObject()) {
                JsonNode htmlNode = root.path("html");
                if (htmlNode.isTextual()) {
                    return parseFromHtml(htmlNode.asText());
                }
                JsonNode listNode = root.path("list");
                if (listNode.isArray()) {
                    return parseFromJsonArray(listNode);
                }
            }
            if (root.isArray()) {
                return parseFromJsonArray(root);
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("failed to parse JSON response", ex);
        }
        return List.of();
    }

    private List<RankingItem> parseFromJsonArray(JsonNode array) {
        List<RankingItem> items = new ArrayList<>();
        for (JsonNode node : array) {
            String brand = textValue(node, "brandName", "brand", "brandNm");
            String product = textValue(node, "productName", "goodsName", "goodsNm", "name");
            String priceText = textValue(node, "price", "salePrc", "salePrice", "prc");
            Long price = parsePrice(priceText);
            Integer rank = parseInt(textValue(node, "rank", "ranking", "rankNo"));
            String productUrl = textValue(node, "productUrl", "url", "goodsUrl");
            String imageUrl = textValue(node, "imageUrl", "imgUrl", "thumbnail");

            if (brand == null || product == null || price == null) {
                continue;
            }
            int resolvedRank = rank == null ? items.size() + 1 : rank;
            items.add(RankingItem.ofParsed(resolvedRank, brand, product, price, productUrl, imageUrl));
        }
        return items;
    }

    private List<RankingItem> parseFromHtml(String html) {
        Document doc = Jsoup.parse(html);
        Elements candidates = doc.select("li:has(.tx_name), li:has(.tx_brand), div:has(.tx_name)");
        if (candidates.isEmpty()) {
            candidates = doc.select("[data-prd-name], [data-goods-name], [data-brand-name]");
        }

        List<RankingItem> items = new ArrayList<>();
        int index = 1;
        for (Element element : candidates) {
            String brand = firstText(element, ".tx_brand", ".brand", "[data-brand-name]", "[data-brand]");
            String product = firstText(element, ".tx_name", ".name", "[data-prd-name]", "[data-goods-name]");
            String priceText = firstText(element, ".tx_cur", ".price", ".prc", "[data-price]");
            Long price = parsePrice(priceText);
            Integer rank = parseInt(firstText(element, ".tx_rank", ".rank", ".num", "[data-rank]"));
            String productUrl = firstAttr(element, "a[href]", "href");
            String imageUrl = firstAttr(element, "img[src], img[data-src]", "src", "data-src");

            if (brand == null || product == null || price == null) {
                log.debug("Skipping item due to missing required fields");
                continue;
            }
            int resolvedRank = rank == null ? index : rank;
            items.add(RankingItem.ofParsed(resolvedRank, brand, product, price, productUrl, imageUrl));
            index++;
        }
        return items;
    }

    private String firstText(Element element, String... selectors) {
        for (String selector : selectors) {
            Element found = element.selectFirst(selector);
            if (found != null) {
                String text = found.text();
                if (!text.isBlank()) {
                    return text.trim();
                }
            }
        }
        return null;
    }

    private String firstAttr(Element element, String selector, String... attrs) {
        Element found = element.selectFirst(selector);
        if (found == null) {
            return null;
        }
        for (String attr : attrs) {
            String value = found.attr(attr);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String textValue(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && value.isValueNode()) {
                String text = value.asText();
                if (!text.isBlank()) {
                    return text.trim();
                }
            }
        }
        return null;
    }

    private Integer parseInt(String text) {
        if (text == null) {
            return null;
        }
        String digits = text.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Long parsePrice(String text) {
        if (text == null) {
            return null;
        }
        String digits = text.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
