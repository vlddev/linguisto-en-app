package com.vlad.linguisto.tools;

import com.vlad.linguisto.db.obj.Inf;

import java.util.List;

public class QuizQuestion {
    Inf inf;
    String question;
    String correctAnswer;
    List<String> answers;

    public Inf getInf() {
        return inf;
    }

    public void setInf(Inf inf) {
        this.inf = inf;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public List<String> getAnswers() {
        return answers;
    }

    public void setAnswers(List<String> answers) {
        this.answers = answers;
    }

}
