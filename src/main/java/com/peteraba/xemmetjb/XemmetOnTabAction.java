package com.peteraba.xemmetjb;

import com.google.common.base.Strings;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
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

        final Project project = e.getProject();

        String emmetExpression = text.substring(start, end);

        // Retrieve HTML Snippet
        boolean isMultiline = emmetExpression.contains("\n");
        boolean isFirstNonWhite = isFirstNonWhiteCharInLine(text, start);

        String mode = getMode((KeyEvent) event);
        Pair<String, Integer> indentation = getIndentation(document, text, start);

        String emmetTemplate = getXemmetTemplate(emmetExpression, mode, isMultiline || isFirstNonWhite, indentation.first, indentation.second);
        if (emmetTemplate.isEmpty()) {
            Messages.showWarningDialog("Can't render the Emmet.HTML as Xemmet did not return any.", "EMMET HTML");

            return;
        }

        // Take action
        Runnable runnable = () -> document.replaceString(start, end, emmetTemplate);

        WriteCommandAction.runWriteCommandAction(project, runnable);
    }

    private String getMode(KeyEvent event) {
        int k = event.getKeyCode();

        return switch (k) {
            case 88 -> "xml";
            case 48 -> "htmx";
            default -> "html";
        };
    }

    private Pair<String, Integer> getIndentation(Document document, @NotNull String text, int start) {
        CommonCodeStyleSettings.IndentOptions indentationOptions = CommonCodeStyleSettings.IndentOptions.retrieveFromAssociatedDocument(document);

        int tabSize = 4;
        boolean useTab = false;
        if (indentationOptions != null) {
            tabSize = indentationOptions.TAB_SIZE;
            useTab = indentationOptions.USE_TAB_CHARACTER;
        }

        char indentChar = ' ';
        if (useTab) {
            indentChar = '\t';
        }

        int depth = 0;

        char c;
        for (int i = 0; i <= start; i++) {
            c = text.charAt(start - i);

            if (c == '\n') {
                break;
            } else if (c != indentChar) {
                depth = 0;
                continue;
            }

            if (i > 0 && i % tabSize == 0) {
                depth++;
            }
        }

        String indentation = Strings.repeat(""+indentChar, tabSize);

        return Pair.pair(indentation, depth);
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

    private @NotNull String getXemmetTemplate(@NotNull String in, @NotNull String mode, boolean isMultiline, @NotNull String indentation, int depth) {
        try {
            BufferedReader reader = getBufferedReader(in, mode, isMultiline, indentation, depth);
            StringBuilder output = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            return output.toString();
        } catch (Exception e) {
            Messages.showWarningDialog(e.getMessage(), "Exception - " + e.getClass().getCanonicalName());
        }

        return in;
    }

    @NotNull
    private static BufferedReader getBufferedReader(@NotNull String in, @NotNull String mode, boolean isMultiline, @NotNull String indentation, int depth) throws IOException {
        ProcessBuilder builder;

        if (isMultiline) {
            builder = new ProcessBuilder("xemmet", "--mode", mode, "--indentation", indentation, "--depth", Integer.toString(depth), in);
        } else {
            builder = new ProcessBuilder("xemmet", "--mode", mode, "--inline", in);
        }

//            builder.redirectErrorStream(true); // Redirects error stream to standard output
        Process process = builder.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        return reader;
    }
}
