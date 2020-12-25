package org.ccg;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.ui.JBColor;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

/**
 * @author chenchonggui
 * @version 1.0
 * @date_time 2020/12/22 21:03
 */
public class ConfigCusor extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent event) {

        PsiFile psiFile = event.getData(LangDataKeys.PSI_FILE);
        if (!"JAVA".equalsIgnoreCase(psiFile.getFileType().getName())) {
            return;
        }
        String configKey = null;
        String tip = "";
        // 获取当前事件触发时，光标所在的元素
        PsiElement psiElement = event.getData(LangDataKeys.PSI_ELEMENT);
        String eleText = psiElement.getText();
        // 根据方法名得到属性值
        int start = eleText.indexOf("get");
        if (start != -1) {
            configKey = eleText.substring(start + 3, eleText.indexOf("("));
        } else {
            start = eleText.indexOf("is");
            if (start != -1) {
                configKey = eleText.substring(start + 2, eleText.indexOf("("));
            } else {
                tip = "未找到配置值！";
                return;
            }
        }
        if (configKey != null) {
            // 首字母小写
            char[] chars = configKey.toCharArray();
            chars[0] += 32;
            Map<String, String> resultMap = findResourceProp(psiFile, String.valueOf(chars));
            if(resultMap == null || resultMap.size() < 1){
                tip = "未找到配置值！";
            }else{
                // 为了展示排版
                int maxLen = 0;
                for (String s : resultMap.keySet()) {
                    String msg = resultMap.get(s).trim() + s.trim();
                    if(msg.length() > maxLen){
                        maxLen = msg.length();
                    }
                }
                maxLen += 12;
                for (String s : resultMap.keySet()) {
                    String msg = resultMap.get(s).trim() + s.trim();
                    tip += "<b>" + resultMap.get(s) + genBlank(maxLen - msg.length()) + "</b>" + s + "<br/>";
                }
            }
        }

        String finalMsg = tip;
        ApplicationManager.getApplication().invokeLater(() -> JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(finalMsg, null,
                        new JBColor(new Color(0xF2F2F2), new Color(0x999999)), null)
                .setFadeoutTime(20000)
                .setHideOnAction(true)
                .createBalloon()
                .show(JBPopupFactory.getInstance().guessBestPopupLocation(event.getData(PlatformDataKeys.EDITOR)),
                        Balloon.Position.below));
    }

    private String genBlank(int n){
        StringBuffer sb = new StringBuffer();
        while(n > 0){
            sb.append("&ensp");
            n--;
        }
        return sb.toString();
    }

    private final String RESOURCES = "resources";

    private Map<String, String> findResourceProp(PsiFile psiFile, String configKey) {
        try {
            String editFilePathStr = psiFile.getVirtualFile().getPath();
            String mainDirPathStr = editFilePathStr.substring(0, editFilePathStr.indexOf("java"));
            String resourcePathStr = mainDirPathStr + RESOURCES;
            File resourceDir = new File(resourcePathStr);
            Map<String, String> resultMap = new HashMap<>();
            if (resourceDir.exists() && resourceDir.isDirectory()) {
                for (File file : resourceDir.listFiles()) {
                    int lineCount = 0;
                    if (file.isFile()) {
                        BufferedReader br = new BufferedReader(new FileReader(file));
                        String line;
                        while ((line = br.readLine()) != null) {
                            lineCount++;
                            if (line.contains(configKey)) {
                                resultMap.put(file.getName() + "  " + lineCount, line.trim());
                            }
                        }
//                    if (file.getName().contains("yml")) {
//                        //
//                    }else if(file.getName().contains("properties")){
//                        // todo
//                    }else{
//                        return null;
//                    }
                    }
                }
                return resultMap;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
