package com.nexaria.launcher.news;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RSSFeedParser {
    private static final Logger logger = LoggerFactory.getLogger(RSSFeedParser.class);
    private static final SimpleDateFormat RSS_DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);

    public static class NewsItem {
        private String title;
        private String description;
        private String link;
        private String content;
        private String author;
        private Date publishDate;

        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getLink() { return link; }
        public String getContent() { return content; }
        public String getAuthor() { return author; }
        public Date getPublishDate() { return publishDate; }

        public String getFormattedDate() {
            if (publishDate == null) return "";
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH);
            return sdf.format(publishDate);
        }
    }

    public static List<NewsItem> fetchNews(String rssUrl) {
        List<NewsItem> items = new ArrayList<>();
        
        try {
            URL url = new URL(rssUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "NexariaLauncher/1.0");

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                logger.warn("RSS feed returned code: {}", responseCode);
                return items;
            }

            InputStream inputStream = connection.getInputStream();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(inputStream);
            doc.getDocumentElement().normalize();

            NodeList itemList = doc.getElementsByTagName("item");

            for (int i = 0; i < itemList.getLength() && i < 5; i++) { // Limit to 5 news
                Element itemElement = (Element) itemList.item(i);
                NewsItem item = new NewsItem();

                item.title = getElementText(itemElement, "title");
                item.description = getElementText(itemElement, "description");
                item.link = getElementText(itemElement, "link");
                item.author = getElementText(itemElement, "dc:creator");

                // Parse content:encoded
                NodeList contentNodes = itemElement.getElementsByTagNameNS("http://purl.org/rss/1.0/modules/content/", "encoded");
                if (contentNodes.getLength() > 0) {
                    item.content = contentNodes.item(0).getTextContent();
                }

                // Parse date
                String pubDateStr = getElementText(itemElement, "pubDate");
                try {
                    item.publishDate = RSS_DATE_FORMAT.parse(pubDateStr);
                } catch (Exception e) {
                    logger.debug("Failed to parse date: {}", pubDateStr);
                }

                items.add(item);
            }

            inputStream.close();
            connection.disconnect();
            
            logger.info("Fetched {} news items from RSS feed", items.size());
        } catch (Exception e) {
            logger.error("Failed to fetch RSS feed", e);
        }

        return items;
    }

    private static String getElementText(Element parent, String tagName) {
        try {
            NodeList nodeList = parent.getElementsByTagName(tagName);
            if (nodeList.getLength() > 0) {
                return nodeList.item(0).getTextContent();
            }
        } catch (Exception e) {
            logger.debug("Failed to get element text for: {}", tagName);
        }
        return "";
    }
}
