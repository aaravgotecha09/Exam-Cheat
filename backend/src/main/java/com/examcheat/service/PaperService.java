package com.examcheat.service;

import com.examcheat.dto.Dtos.*;
import com.examcheat.model.PaperBank;
import com.examcheat.model.Question;
import com.examcheat.model.TestPaper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/** Serves papers WITHOUT the answer key. */
@Service
public class PaperService {

    public List<PaperSummary> list() {
        List<PaperSummary> out = new ArrayList<>();
        for (TestPaper p : PaperBank.all()) {
            out.add(new PaperSummary(p.getId(), p.getPaperName(), p.getQuestionCount(), p.getTotalMarks()));
        }
        return out;
    }

    public PaperDetail detail(int paperId) {
        TestPaper p = PaperBank.getPaper(paperId);   // InvalidChoiceException -> 400
        List<QuestionView> qs = new ArrayList<>();
        for (int i = 1; i <= p.getQuestionCount(); i++) {
            qs.add(questionView(p, i, null));
        }
        return new PaperDetail(p.getId(), p.getPaperName(), p.getTotalMarks(), qs);
    }

    public static QuestionView questionView(TestPaper p, int number, String selected) {
        Question q = p.getQuestion(number - 1);
        List<OptionView> options = new ArrayList<>();
        String[] texts = q.getOptions();
        for (int i = 0; i < texts.length; i++) {
            options.add(new OptionView(String.valueOf((char) ('A' + i)), texts[i]));
        }
        return new QuestionView(number, p.getQuestionCount(), q.getText(), q.getMarks(), options, selected);
    }
}
