package com.examcheat.model;

import com.examcheat.exception.InvalidChoiceException;
import java.util.ArrayList;
import java.util.List;

/** Holds the 3 sample test papers (5 MCQs x 2 marks) exactly as in the project document. */
public final class PaperBank {
    private PaperBank() {}

    private static Question makeQuestion(String text, String a, String b, String c, String d, char correct) {
        String[] opts = {a, b, c, d};
        return new Question(text, opts, correct, 2);
    }

    public static List<TestPaper> all() {
        List<TestPaper> list = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            list.add(getPaper(i));
        }
        return list;
    }

    public static TestPaper getPaper(int choice) {
        if (choice < 1 || choice > 3) {
            throw new InvalidChoiceException("Paper choice must be between 1 and 3.");
        }
        Question[] q = new Question[5];
        if (choice == 1) {
            q[0] = makeQuestion("Which keyword is used to inherit a class in Java?",
                    "implements", "super", "extends", "inherits", 'C');
            q[1] = makeQuestion("Which feature is used to achieve runtime polymorphism?",
                    "Method overriding", "Method overloading", "Constructor overloading", "Static binding", 'A');
            q[2] = makeQuestion("Which keyword is used to create an object?",
                    "class", "new", "this", "object", 'B');
            q[3] = makeQuestion("Which block always executes whether or not an exception occurs?",
                    "try", "catch", "throw", "finally", 'D');
            q[4] = makeQuestion("A class that cannot be instantiated and may contain abstract methods is called?",
                    "Interface", "Final class", "Static class", "Abstract class", 'D');
            return new TestPaper(1, "Java Programming", q);
        } else if (choice == 2) {
            q[0] = makeQuestion("Which data structure follows the LIFO principle?",
                    "Queue", "Array", "Stack", "Tree", 'C');
            q[1] = makeQuestion("Which data structure follows the FIFO principle?",
                    "Queue", "Stack", "Graph", "Tree", 'A');
            q[2] = makeQuestion("What is the worst case time complexity of binary search?",
                    "O(n)", "O(n^2)", "O(1)", "O(log n)", 'D');
            q[3] = makeQuestion("Which stack operation removes the top element?",
                    "push", "pop", "peek", "insert", 'B');
            q[4] = makeQuestion("In a singly linked list, each node contains data and what else?",
                    "An index number", "A hash value", "A reference to the next node", "The array size", 'C');
            return new TestPaper(2, "Data Structures", q);
        } else {
            q[0] = makeQuestion("Which SQL command is used to retrieve data from a table?",
                    "SELECT", "INSERT", "UPDATE", "DELETE", 'A');
            q[1] = makeQuestion("A primary key must contain?",
                    "Duplicate values", "NULL values", "Unique and non-null values", "Only numbers", 'C');
            q[2] = makeQuestion("Which normal form removes partial dependency?",
                    "1NF", "2NF", "3NF", "BCNF", 'B');
            q[3] = makeQuestion("Which command removes a table completely from the database?",
                    "DELETE", "TRUNCATE", "DROP", "REMOVE", 'C');
            q[4] = makeQuestion("Which SQL clause is used to filter rows?",
                    "GROUP BY", "ORDER BY", "JOIN", "WHERE", 'D');
            return new TestPaper(3, "DBMS", q);
        }
    }
}
