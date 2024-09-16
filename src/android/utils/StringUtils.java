package org.apache.cordova.firebase.utils;

import android.content.Context;

import android.os.Build;

import android.graphics.Typeface;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import android.util.Pair;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class StringUtils {

    public static String getStringResource(Context context, String name) {
        return context.getString(
                context.getResources().getIdentifier(
                        name, "string", context.getPackageName()
                )
        );
    }

    public static Spannable getColorizedText(Context context, String stringResName, String colorName) {
        Spannable spannable = new SpannableString(getStringResource(context, stringResName));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            spannable.setSpan(
                new ForegroundColorSpan(context.getColor(context.getResources().getIdentifier(colorName, "color", context.getPackageName()))),
                0, 
                spannable.length(), 
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        }
        
        return spannable;
    }    
    
    public static SpannableString getHightlitedMentions(String text, List<String> mentions) {
        SpannableString sb = new SpannableString(text);

        Set<Pair<Integer, Integer>> boldParts = new HashSet<>();

        text = text.toLowerCase(Locale.ENGLISH);

        for (String mention : mentions) {
            String mentionUserName = mention.substring(0, mention.indexOf('@'));

            if (text.contains("@" + mentionUserName)) {
                for (int i = 0; i < text.length() - 1; i++) {
                    int j = text.indexOf("@" + mentionUserName, i);
                    if (j != -1) {
                        int endIndex = j + (mention).length() + 1;

                        boldParts.add(new Pair<>(j, endIndex));
                    }
                }
            }

            String[] mentionParts = mentionUserName.split("\\.");

            for (int i = 0; i < text.length() - 1; i++) {
                int startIndex = text.indexOf(mentionParts[0], i);
                if (startIndex == -1) {
                    continue;
                }

                int spaceIndex = startIndex + mentionParts[0].length();

                String secondName = text.substring(spaceIndex + 1, spaceIndex + 1 + mentionParts[1].length());

                if (secondName.equalsIgnoreCase(mentionParts[1])) {
                    boldParts.add(new Pair<>(startIndex - 1, spaceIndex + mentionParts[1].length() + 1));
                } else {
                    boldParts.add(new Pair<>(startIndex - 1, startIndex + mentionParts[0].length()));
                }
            }
        }

        for (int i = 0; i < text.length() - 1; i++) {
            int startIndex = text.indexOf("@all".toLowerCase(Locale.ENGLISH), i);
            if (startIndex != -1) {
                boldParts.add(new Pair<>(startIndex, startIndex + 4));
            }
        }

        for (Pair<Integer, Integer> index : boldParts) {
            sb.setSpan(new StyleSpan(Typeface.BOLD), index.first, index.second, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return sb;
    }

    public static String getMD5forString(String source) {
        String result = null;

        try {
            // Create MD5 Hash
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(source.getBytes());
            byte[] messageDigest = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String h = Integer.toHexString(0xFF & b);
                while (h.length() < 2) {
                    h = "0" + h;
                }

                hexString.append(h);
            }

            result = hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return result;
    }
}
