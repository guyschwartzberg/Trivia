package il.ac.tau.cs.sw1.trivia;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class TriviaGUI {

	private static final int MAX_ERRORS = 3;
	private Shell shell;
	private Label scoreLabel;
	private Composite questionPanel;
	private Label startupMessageLabel;
	private Font boldFont;
	private String lastAnswer;
	private LinkedHashMap<String, List<String>> possibleQuestions;
	private List<String> questions;
	private List<List<String>> answerList;
	private int current = 0;
	private boolean gameOver = false;
	private Integer score = 0;
	private int consecutivelyWrong = 0;
	private int questionsAnswered;
	private boolean usedPass = false;
	private boolean usedFiftyFifty = false;
	// Currently visible UI elements.
	Label instructionLabel;
	Label questionLabel;
	private List<Button> answerButtons = new LinkedList<>();
	private Button passButton;
	private Button fiftyFiftyButton;

	public void open() {
		createShell();
		runApplication();
	}

	/**
	 * Creates the widgets of the application main window
	 */
	private void createShell() {
		Display display = Display.getDefault();
		shell = new Shell(display);
		shell.setText("Trivia");

		// window style
		Rectangle monitor_bounds = shell.getMonitor().getBounds();
		shell.setSize(new Point(monitor_bounds.width / 3,
				monitor_bounds.height / 4));
		shell.setLayout(new GridLayout());

		FontData fontData = new FontData();
		fontData.setStyle(SWT.BOLD);
		boldFont = new Font(shell.getDisplay(), fontData);

		// create window panels
		createFileLoadingPanel();
		createScorePanel();
		createQuestionPanel();
	}

	/**
	 * Creates the widgets of the form for trivia file selection
	 */
	private void createFileLoadingPanel() {
		final Composite fileSelection = new Composite(shell, SWT.NULL);
		fileSelection.setLayoutData(GUIUtils.createFillGridData(1));
		fileSelection.setLayout(new GridLayout(4, false));

		final Label label = new Label(fileSelection, SWT.NONE);
		label.setText("Enter trivia file path: ");

		// text field to enter the file path
		final Text filePathField = new Text(fileSelection, SWT.SINGLE
				| SWT.BORDER);
		filePathField.setLayoutData(GUIUtils.createFillGridData(1));

		// "Browse" button
		final Button browseButton = new Button(fileSelection, SWT.PUSH);
		browseButton.setText("Browse");
		browseButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				String path = GUIUtils.getFilePathFromFileDialog(shell);
				if (path != null)
					filePathField.setText(path);	
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				
			}
		
		
		});

		// "Play!" button
		final Button playButton = new Button(fileSelection, SWT.PUSH);
		playButton.setText("Play!");
		playButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
					try {
						possibleQuestions = readFile(filePathField.getText());
					} catch (IOException e) {
						possibleQuestions = null;
					}
					score = 0;
					gameOver = false;
					consecutivelyWrong = 0;
					current = 0;
					lastAnswer = "";
					questionsAnswered = 0;
					usedPass = false;
					usedFiftyFifty = false;
					answerButtons.clear();
					possibleQuestions = shuffleMap(possibleQuestions);
					questions = new ArrayList<String>(possibleQuestions.keySet());
					answerList = new ArrayList<List<String>>(possibleQuestions.values());
					updateQuestionPanel(questions.get(current), answerList.get(current));
					scoreLabel.setText(score.toString());
				}		
				

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				
			}
		
		
		});
	}
	
	private LinkedHashMap<String, List<String>> shuffleMap(LinkedHashMap<String, List<String>> hmap){		 
		// Get all the entries in the map into a list
		List<Map.Entry<String, List<String>>> entries = new ArrayList<>(hmap.entrySet());
		 
		// Shuffle the list
		Collections.shuffle(entries);
		 
		// Insert them all into a LinkedHashMap
		LinkedHashMap<String, List<String>> shuffledWindow = new LinkedHashMap<String, List<String>>();
		for (Map.Entry<String, List<String>> entry : entries) {
		    shuffledWindow.put(entry.getKey(), entry.getValue());
		}
		
		return shuffledWindow;
	}
	
	private LinkedHashMap<String, List<String>> readFile(String path) throws IOException {
		 File file = new File(path);
		 LinkedHashMap<String, List<String>> hmap = new LinkedHashMap<String, List<String>>();
		 BufferedReader br = new BufferedReader(new FileReader(file));
		 
		 String st;
		 while ((st = br.readLine()) != null)
		 {
			 List<String> tokens = Arrays.asList(st.split("\t"));
			 hmap.put(tokens.get(0), tokens.subList(1, tokens.size()));
		 }
		 br.close();
		 return hmap;
	}

	/**
	 * Creates the panel that displays the current score
	 */
	private void createScorePanel() {
		Composite scorePanel = new Composite(shell, SWT.BORDER);
		scorePanel.setLayoutData(GUIUtils.createFillGridData(1));
		scorePanel.setLayout(new GridLayout(2, false));

		final Label label = new Label(scorePanel, SWT.NONE);
		label.setText("Total score: ");

		// The label which displays the score; initially empty
		scoreLabel = new Label(scorePanel, SWT.NONE);
		scoreLabel.setLayoutData(GUIUtils.createFillGridData(1));
	}

	/**
	 * Creates the panel that displays the questions, as soon as the game
	 * starts. See the updateQuestionPanel for creating the question and answer
	 * buttons
	 */
	private void createQuestionPanel() {
		questionPanel = new Composite(shell, SWT.BORDER);
		questionPanel.setLayoutData(new GridData(GridData.FILL, GridData.FILL,
				true, true));
		questionPanel.setLayout(new GridLayout(2, true));

		// Initially, only displays a message
		startupMessageLabel = new Label(questionPanel, SWT.NONE);
		startupMessageLabel.setText("No question to display, yet.");
		startupMessageLabel.setLayoutData(GUIUtils.createFillGridData(2));
	}

	/**
	 * Serves to display the question and answer buttons
	 */
	private void updateQuestionPanel(String question, List<String> answers) {
		// Save current list of answers.
		List<String> currentAnswers = answers;
		
		// clear the question panel
		Control[] children = questionPanel.getChildren();
		for (Control control : children) {
			control.dispose();
		}

		// create the instruction label
		instructionLabel = new Label(questionPanel, SWT.CENTER | SWT.WRAP);
		instructionLabel.setText(lastAnswer + "Answer the following question:");
		instructionLabel.setLayoutData(GUIUtils.createFillGridData(2));

		// create the question label
		questionLabel = new Label(questionPanel, SWT.CENTER | SWT.WRAP);
		questionLabel.setText(question);
		questionLabel.setFont(boldFont);
		questionLabel.setLayoutData(GUIUtils.createFillGridData(2));
		String correct = answers.get(0);
		Collections.shuffle(answers);
		// create the answer buttons
		for (int i = 0; i < 4; i++) {
			Button answerButton = new Button(questionPanel, SWT.PUSH | SWT.WRAP);
			answerButton.setText(answers.get(i));
			GridData answerLayoutData = GUIUtils.createFillGridData(1);
			answerLayoutData.verticalAlignment = SWT.FILL;
			answerButton.setLayoutData(answerLayoutData);
			answerButton.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					Advance(true, answerButton, correct);
				
				}
				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {	
				}
			});
			answerButtons.add(answerButton);
		}

		// create the "Pass" button to skip a question
		passButton = new Button(questionPanel, SWT.PUSH);
		passButton.setText("Pass");
		GridData data = new GridData(GridData.END, GridData.CENTER, true,
				false);
		data.horizontalSpan = 1;
		passButton.setLayoutData(data);
		if (score <= 0 && usedPass == true)
			passButton.setEnabled(false);
		else
			passButton.setEnabled(true);
		passButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				Advance(false, null, null);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {	
			}
		});
		
		// create the "50-50" button to show fewer answer options
		fiftyFiftyButton = new Button(questionPanel, SWT.PUSH);
		fiftyFiftyButton.setText("50-50");
		data = new GridData(GridData.BEGINNING, GridData.CENTER, true,
				false);
		data.horizontalSpan = 1;
		fiftyFiftyButton.setLayoutData(data);
		if (score <= 0 && usedFiftyFifty == true)
			fiftyFiftyButton.setEnabled(false);
		else
			fiftyFiftyButton.setEnabled(true);
		fiftyFiftyButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				String[] wrongAnswers = pickRandomWrongAnswers(currentAnswers, correct);
				for (int i = 0; i < 2; i++)
				{
					for (Button but : answerButtons)
					{
						if (but.getText().equals(wrongAnswers[i]))
						{
							but.setEnabled(false);
							break;
						}
					}
				}
				if (usedFiftyFifty == true)
					score--;
				scoreLabel.setText(score.toString());
				usedFiftyFifty = true;
				fiftyFiftyButton.setEnabled(false);
				
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {	
			}
		});

		// two operations to make the new widgets display properly
		questionPanel.pack();
		questionPanel.getParent().layout();
	}

	/**
	 * Opens the main window and executes the event loop of the application
	 */
	
	private String[] pickRandomWrongAnswers(List<String> answers, String correct){
		Random r = new Random();
		String [] wrongAnswers = new String[2];
		int i = 0;
		while (i != 2)
		{
			int num = r.nextInt(4);
			if (!answers.get(num).equals(correct) && !answers.get(num).equals(wrongAnswers[0]))
			{
				wrongAnswers[i] = answers.get(num);	
				i++;
			}
		}
		return wrongAnswers;
			
	}
	
	private void Advance(boolean answeredOrPassed, Button b, String answer){
		if (gameOver == false)
		{
			if (answeredOrPassed == true)
			{
				questionsAnswered++;;
				if (b.getText().equals(answer))
				{
					lastAnswer = "Correct! ";
					score += 3;
					consecutivelyWrong = 0;
				}
				else {
					lastAnswer = "Wrong... ";
					score -= 2;
					consecutivelyWrong++;
				}
			}
			else 
			{
				lastAnswer = "";
				if (usedPass)
					score--;
				else
					usedPass = true;
			}
			scoreLabel.setText(score.toString());
			current++;
			if (consecutivelyWrong == MAX_ERRORS || current == questions.size())
			{
				GUIUtils.showInfoDialog(shell, "GAME OVER", "Your final score is " + score + " After " + questionsAnswered + " questions");
				gameOver = true;
			}
			
			if (gameOver == false)
				answerButtons.clear();
				updateQuestionPanel(questions.get(current), answerList.get(current));
		}
	}
	private void runApplication() {
		shell.open();
		Display display = shell.getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
		boldFont.dispose();
	}
}
