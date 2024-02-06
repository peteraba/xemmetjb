package com.peteraba.xemmetjb;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class XemmetOnTabAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        InputEvent event = e.getInputEvent();
        if (!(event instanceof KeyEvent)) {
            return;
        }

        int k = ((KeyEvent) event).getKeyCode();

        String mode = "--mode=html";
        switch (k) {
            case 88:
                mode = "--mode=xml";
                break;
            case 48:
                mode = "--mode=htmx";
        }

        // Get editor
        final Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);

        // Get document
        final Document document = editor.getDocument();
        String text = document.getText();

        // Get cursor
        final CaretModel caretModel = editor.getCaretModel();
        int caretOffset = caretModel.getOffset();

        // Get Emmet snippet
        int start = getStart(text, caretOffset);
        int end = getEnd(text, caretOffset);

        if (end <= start) {
            return;
        }

        String emmetExpression = text.substring(start, end);

        // Retrieve HTML Snippet
        boolean isMultiline = emmetExpression.contains("\n");
        boolean isFirstNonWhite = isFirstNonWhiteCharInLine(text, start);

        String emmetTemplate = getXemmetTemplate(emmetExpression, mode, isMultiline || isFirstNonWhite);
        if (emmetTemplate.isEmpty()) {
            Messages.showWarningDialog("Can't render the Emmet.HTML as Xemmet did not return any.", "EMMET HTML");

            return;
        }

        // Take action
        Runnable runnable = () -> document.replaceString(start, end, emmetTemplate);

        final Project project = e.getProject();
        WriteCommandAction.runWriteCommandAction(project, runnable);
    }

    private enum State {
        NORMAL, TEXT, ATTRIBUTE
    }

    private boolean isFirstNonWhiteCharInLine(@NotNull String text, int start) {
        char c;

        while (start > 0) {
            c = text.charAt(start - 1);

            if (c == '\n') {
                return true;
            }

            if (c != ' ' && c != '\t' && c != '\r') {
                return false;
            }

            start--;
        }

        return true;
    }

    private int getStart(@NotNull String text, int start) {
        State state = State.NORMAL;
        char c;

        while (start > 0) {
            c = text.charAt(start - 1);

            boolean br = false;

            switch (state) {
                case TEXT:
                    if (c == '{') {
                        state = State.NORMAL;
                    }

                    break;

                case ATTRIBUTE:
                    if (c == '[') {
                        state = State.NORMAL;
                    }

                    break;

                default:
                    if (c == ']') {
                        state = State.ATTRIBUTE;
                    } else if (c == '}') {
                        state = State.TEXT;
                    } else if (Character.isWhitespace(c)) {
                        br = true;
                    }

                    break;
            }

            if (br) {
                break;
            }

            start--;
        }

        if (state != State.NORMAL) {
            Messages.showWarningDialog("Please ensure to have the cursor at the beginning or end of the emmet expression.", "Invalid Emmet Expression Found");

            return 0;
        }

        return start;
    }

    private int getEnd(@NotNull String text, int end) {
        State state = State.NORMAL;
        char c;

        while (end < text.length()) {
            c = text.charAt(end);

            boolean br = false;

            switch (state) {
                case TEXT:
                    if (c == '}') {
                        state = State.NORMAL;
                    }

                    break;

                case ATTRIBUTE:
                    if (c == ']') {
                        state = State.NORMAL;
                    }

                    break;

                default:
                    if (c == '[') {
                        state = State.ATTRIBUTE;
                    } else if (c == '{') {
                        state = State.TEXT;
                    } else if (Character.isWhitespace(c)) {
                        br = true;
                    }

                    break;
            }

            if (br) {
                break;
            }

            end++;
        }

        if (state != State.NORMAL) {
            Messages.showWarningDialog("Please ensure to have the cursor at the beginning or end of the emmet expression.", "Invalid Emmet Expression Found");

            return 0;
        }

        return end;
    }

    private @NotNull String getXemmetTemplate(@NotNull String in, @NotNull String mode, boolean isMultiline) {
        try {
            BufferedReader reader = getBufferedReader(in, mode, isMultiline);
            StringBuilder output = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            return output.toString().replace("'  '", "    ");
        } catch (Exception e) {
            Messages.showWarningDialog(e.getMessage(), "Error: " + e.getClass().getCanonicalName());
        }

        return "FATAL ERROR";
    }

    @NotNull
    private static BufferedReader getBufferedReader(@NotNull String in, @NotNull String mode, boolean isMultiline) throws IOException {
        ProcessBuilder builder;

        if (isMultiline) {
            builder = new ProcessBuilder("xemmet", mode, "--indentation='  '", in);
        } else {
            builder = new ProcessBuilder("xemmet", mode, "--inline", in);
        }

//            builder.redirectErrorStream(true); // Redirects error stream to standard output
        Process process = builder.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        return reader;
    }
}
