package org.appxi.dictionary;

import java.io.InputStream;
import java.util.Base64;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DictionaryHelper {
    private static final Pattern PAT_PIC = Pattern.compile("(.*)(<PIC>[a-zA-Z\\d]+[.](jpg|JPG|png|PNG|bmp|BMP|gif|GIF)</PIC>)(.*)");

    public static String toTextDocument(DictEntry entry) {
        if (null == entry.title()) {
            entry.model.readEntry(entry);
        }

        if (null == entry.content && !entry.isCategory()) {
            entry.model.readEntryContentText(entry);
        }

        return entry.title()
               + "\n" + entry.contentText()
               + "\n——【" + entry.dictionary().getName() + "】";
    }

    public static String toHtmlDocument(DictEntry entry) {
        if (null == entry.title()) {
            entry.model.readEntry(entry);
        }

        if (null == entry.content && !entry.isCategory()) {
            entry.model.readEntryContentText(entry);
        }

        String content = entry.isCategory() ? "<>" : entry.contentText();
        //
        content = content.replace("<SEEALSO>", "<a href=\"javascript:void();\" onclick=\"_dict_SeeAlso('" + entry.dictionary().id + "', this)\">");
        content = content.replace("</SEEALSO>", "</a>");
        //
        // 特殊标记标准化
        content = content.replace("【】：", "【词义】：");
        content = content.replace("【derivation】：", "【词源】：");
        content = content.replace("【strokes】：", "【笔画】：");
        content = content.replace("【pinyin】：", "【拼音】：");
        content = content.replace("【abbreviation】：", "【简拼】：");
        content = content.replace("【radicals】：", "【部首】：");
        content = content.replace("【example】：", "【示例】：");
        content = content.replace("【more】：", "【更多】：");
        content = content.replace("【oldword】：", "【古字】：");
        // 处理换行和缩进，保持风格易于阅读
        content = content.replaceAll("(\r\n)|[\r\n]", "<br>");
        if (content.contains("又【")) {
            content = content.replaceAll("([^】又>])【", "$1<br>　【");
            content = content.replace("<br>【", "<br>　【");
            content = content.replaceFirst("^〔", "　〔");
            content = content.replaceFirst("^【", "　【");
        }
        if (content.contains("。　又")) {
            content = content.replace("。　又", "。<br>又");
        }
        //
        DictionaryResource dictionaryResource = null;
        while (true) {
            Matcher matcher = PAT_PIC.matcher(content);
            if (!matcher.matches()) {
                break;
            }
            if (null == dictionaryResource) {
                dictionaryResource = new DictionaryResource(entry.dictionary());
            }

            try {
                String picPart = matcher.group(2);
                String picName = picPart.substring(5, picPart.length() - 6);

                InputStream stream = dictionaryResource.getInputStream(picName);
                if (null != stream) {
                    byte[] imgBytes = stream.readAllBytes();

                    String imgType = picName.substring(picName.indexOf('.') + 1).toLowerCase(Locale.ROOT);
                    String imgData = "data:image/".concat(imgType).concat(";base64,").concat(Base64.getEncoder().encodeToString(imgBytes));
                    String imgPart = "<img src=\"" + imgData + "\" />";

                    content = content.replace(picPart, imgPart);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (null != dictionaryResource) {
            try {
                dictionaryResource.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //
        return "<div style=\"display: block;\" data-dict=\"" + entry.dictionary().id + "|" + entry.dictionary().getName() + "\">"
               + "<h2 style=\"text-align: center;\">"
               + "<a href=\"javascript:void();\" onclick=\"_dict_SeeAlso('" + entry.dictionary().id + "', this)\">"
               + entry.title()
               + "</a>"
               + "</h2>"
               + "<div style=\"text-align: center;\">——《" + entry.dictionary().getName() + "》</div>"
               + "<p>" + content + "</p>"
               + "<hr>"
               + "</div>";
    }
}
