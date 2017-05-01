package MutationTesting.MutationTesting;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.text.BadLocationException;

public class Driver {


	static int mutants_count = 0;

	public static void main(String[] args) throws IOException, CoreException, BadLocationException{
		Report.getLoc();
		Collection<File> files = Report.getFiles();

		for(File file: files){
			makeReport("Parsing File: "+file.getName());
			readThrough(file);
			Report.appendReport();
		}
		//		FileAndReportUtils.printReport();
		Report.saveReport();
	}

	private static void makeReport(String str){
		Report.makeReport(str);
	}

	private static void readThrough(File file) throws CoreException, IOException, BadLocationException {

		makeReport("*********Generating mutants for file "+file+"**********");
		ArrayList<String>mutants = createProjMutants(file);
		parseMutants(file, mutants);
		System.out.println("*********Generated mutants for file "+file+"**********");
	}


	private static void parseMutants(File file, ArrayList<String> mutants) throws BadLocationException, IOException {

		
		for(int i=1; i<=mutants.size(); i++){

			String [] fileMutants = null;
			// for windows system
			if(File.separator.equals("\\")){  ///////////////////////////////////////////
				fileMutants = file.getAbsolutePath().split(Report.getProjPreLoc().toString().replace("\\", "\\\\"));
			}
			// for linux systems
			else{
				fileMutants = file.getAbsolutePath().split(Report.getProjPreLoc().toString().replace("\\", "/"));
			}

			int mutant_localcount = mutants_count+i;
			String curFileLocation = Report.getMutantLoc()+File.separator+Report.getProjName()+"-"+mutant_localcount+fileMutants[1];
			Report.createMutantProject(mutants_count+i);
			int start = Integer.parseInt(getMutantsInfo(mutants.get(i-1), 1));
			int length = Integer.parseInt(getMutantsInfo(mutants.get(i-1), 2));
			String new_start = getMutantsInfo(mutants.get(i-1), 4);
			String mutated_contents = generateMutatedCode(start, length, new_start, FileUtils.readFileToString(new File(file.getAbsolutePath())));

			FileWriter fileOverwrite = new FileWriter(curFileLocation, false);
			fileOverwrite.write(mutated_contents);
			fileOverwrite.close();

		}
		mutants_count += mutants.size();
		makeReport("Mutants Size: "+ mutants.size());
		makeReport("Total Mutants: "+ mutants_count);
		//		mt.copyProject(fname, "_mutant",mutated_contents);
	}

	private static String generateMutatedCode(int start, int length, String new_start, String contents) throws MalformedTreeException, BadLocationException
	{

		Document document = new Document(contents);
		CompilationUnit astRoot = parseStringToCompilationUnit(contents);
		AST ast = astRoot.getAST();
		ASTRewrite rewriter = ASTRewrite.create(ast);
		TypeDeclaration typeDecl = (TypeDeclaration) astRoot.types().get(0);

		//search for the node to be replaced
		NodeFinder myNodeFinder = new NodeFinder(typeDecl, start, length);
		ASTNode oldNode = myNodeFinder.getCoveredNode();

		//Create a new node using the provided source code
		TextElement siso = ast.newTextElement();
		siso.setText(new_start);

		//replace the node
		rewriter.replace(oldNode, siso, null);
		TextEdit edits = rewriter.rewriteAST(document,null);
		edits.apply(document);
		//		
		//		System.out.println(document.get());
		return document.get();
	}

	private static String getMutantsInfo(String mutant, int index) 
	{
		String[] splits = mutant.split("\n");
		String currentSplit = splits[index];
		int indexOfLabel = currentSplit.indexOf(": ") + 2;
		String info = currentSplit.substring(indexOfLabel);
		return info;
	}


	private static ArrayList<String> createProjMutants(File file) throws CoreException, IOException
	{

		String mutationPlanPath = Report.getMutantsloc()+File.separator+file.getName()+"_mutants.txt";
		makeReport("Mutant File Location: "+mutationPlanPath);
		@SuppressWarnings("deprecation")
		String contents = FileUtils.readFileToString(new File(file.getAbsolutePath()));
		//		System.out.println(contents);
		//Generate mutants
		final ArrayList<String> mutants = new ArrayList<String>();

		try 
		{  
			final CompilationUnit astRoot = parseStringToCompilationUnit(contents);
			AST ast = astRoot.getAST();
			ASTRewrite.create(ast);

			//Each TypeDeclaration also seems to represent a class
			if(astRoot.types().size()>0){
				TypeDeclaration typeDecl = (TypeDeclaration) astRoot.types().get(0);

				//Get all methods from the class
				MethodDeclaration[] methodDeclarations = typeDecl.getMethods();

				for (final MethodDeclaration methodDeclaration : methodDeclarations) 
				{

					Block methodBody = methodDeclaration.getBody();
					if(methodBody!=null){
						methodBody.accept(new ASTVisitor() 
						{  

							//postfix Expressions
							public boolean visit(PostfixExpression node) 
							{
								StringBuilder stringbuf = new StringBuilder();
								IMethodBinding a = methodDeclaration.resolveBinding();
								stringbuf.append("Line: " + astRoot.getLineNumber(node.getStartPosition()) + "\n");
								stringbuf.append("Start Position: " + node.getStartPosition() + "\n");
								stringbuf.append("Length: " + node.getLength() + "\n");
								stringbuf.append("Current source: " + node + "\n");
								if(node.getOperator().toString().equals("++")){
									node.setOperator(PostfixExpression.Operator.toOperator("--"));
								}
								else{
									node.setOperator(PostfixExpression.Operator.toOperator("++"));
								}
								stringbuf.append("New source: " + node + "\n");
								stringbuf.append("\n");
								mutants.add(stringbuf.toString());
								return true; 
							} 
							public boolean visit(PrefixExpression node) 
							{
								StringBuilder stringbuf = new StringBuilder();
								IMethodBinding a = methodDeclaration.resolveBinding();
								
								String cur = node.toString();
								if(node.getOperator().toString().equals("!")){
									
									stringbuf.append("Line: " + astRoot.getLineNumber(node.getStartPosition()) + "\n");
									stringbuf.append("Start Position: " + node.getStartPosition() + "\n");
									stringbuf.append("Length: " + node.getLength() + "\n");
									stringbuf.append("Current source: " + cur + "\n");
									node.setOperator(PrefixExpression.Operator.toOperator("--"));
									stringbuf.append("New source: " + node + "\n");
									stringbuf.append("\n");
									mutants.add(stringbuf.toString());
									
									stringbuf = new StringBuilder();
									stringbuf.append("Line: " + astRoot.getLineNumber(node.getStartPosition()) + "\n");
									stringbuf.append("Start Position: " + node.getStartPosition() + "\n");
									stringbuf.append("Length: " + node.getLength() + "\n");
									stringbuf.append("Current source: " + cur + "\n");
									node.setOperator(PrefixExpression.Operator.toOperator("++"));
									stringbuf.append("New source: " + node + "\n");
									stringbuf.append("\n");
									mutants.add(stringbuf.toString());
									
//									stringbuf = new StringBuilder();
//									stringbuf.append("Line: " + astRoot.getLineNumber(node.getStartPosition()) + "\n");
//									stringbuf.append("Start Position: " + node.getStartPosition() + "\n");
//									stringbuf.append("Length: " + node.getLength() + "\n");
//									stringbuf.append("Current source: " + node + "\n");
//									node.setOperator(PrefixExpression.Operator.toOperator("true"));
//									stringbuf.append("New source: " + node + "\n");
//									stringbuf.append("\n");
//									mutants.add(stringbuf.toString());
									
									return true; 
								}
								else if(node.getOperator().toString().equals("--")){
									stringbuf = new StringBuilder();
									stringbuf.append("Line: " + astRoot.getLineNumber(node.getStartPosition()) + "\n");
									stringbuf.append("Start Position: " + node.getStartPosition() + "\n");
									stringbuf.append("Length: " + node.getLength() + "\n");
									stringbuf.append("Current source: " + cur + "\n");
									node.setOperator(PrefixExpression.Operator.toOperator("++"));
									stringbuf.append("New source: " + node + "\n");
									stringbuf.append("\n");
									mutants.add(stringbuf.toString());
									
									return true;
									
								}
								else if(node.getOperator().toString().equals("++")){
									stringbuf = new StringBuilder();
									stringbuf.append("Line: " + astRoot.getLineNumber(node.getStartPosition()) + "\n");
									stringbuf.append("Start Position: " + node.getStartPosition() + "\n");
									stringbuf.append("Length: " + node.getLength() + "\n");
									stringbuf.append("Current source: " + cur + "\n");
									node.setOperator(PrefixExpression.Operator.toOperator("--"));
									stringbuf.append("New source: " + node + "\n");
									stringbuf.append("\n");
									mutants.add(stringbuf.toString());
									
									return true;
									
								}
								else if(node.getOperator().toString().equals("-")){
									stringbuf = new StringBuilder();
									stringbuf.append("Line: " + astRoot.getLineNumber(node.getStartPosition()) + "\n");
									stringbuf.append("Start Position: " + node.getStartPosition() + "\n");
									stringbuf.append("Length: " + node.getLength() + "\n");
									stringbuf.append("Current source: " + cur + "\n");
									node.setOperator(PrefixExpression.Operator.toOperator("+"));
									stringbuf.append("New source: " + node + "\n");
									stringbuf.append("\n");
									mutants.add(stringbuf.toString());
									
									stringbuf = new StringBuilder();
									stringbuf.append("Line: " + astRoot.getLineNumber(node.getStartPosition()) + "\n");
									stringbuf.append("Start Position: " + node.getStartPosition() + "\n");
									stringbuf.append("Length: " + node.getLength() + "\n");
									stringbuf.append("Current source: " + cur + "\n");
									node.setOperator(PrefixExpression.Operator.toOperator("--"));
									stringbuf.append("New source: " + node + "\n");
									stringbuf.append("\n");
									mutants.add(stringbuf.toString());
									
									stringbuf = new StringBuilder();
									stringbuf.append("Line: " + astRoot.getLineNumber(node.getStartPosition()) + "\n");
									stringbuf.append("Start Position: " + node.getStartPosition() + "\n");
									stringbuf.append("Length: " + node.getLength() + "\n");
									stringbuf.append("Current source: " + cur + "\n");
									node.setOperator(PrefixExpression.Operator.toOperator("++"));
									stringbuf.append("New source: " + node + "\n");
									stringbuf.append("\n");
									mutants.add(stringbuf.toString());
									
									return true;
								}
								
								return true;
								
							} 


							//try for + how does it work?
							public boolean visit(InfixExpression node){
								IMethodBinding a = methodDeclaration.resolveBinding();
								//minus Operator
								String cur = node.toString();

								if(node.getOperator().toString().equals("-")){
									StringBuilder stringbuf = new StringBuilder();
									stringbuf.append("Line: " + astRoot.getLineNumber(node.getStartPosition()) + "\n");
									stringbuf.append("Start Position: " + node.getStartPosition() + "\n");
									stringbuf.append("Length: " + node.getLength() + "\n");
									stringbuf.append("Current source: " + cur +"\n");
									node.setOperator(InfixExpression.Operator.PLUS);
									stringbuf.append("New source: " + node + "\n");
									stringbuf.append("\n");
									mutants.add(stringbuf.toString());

									stringbuf = new StringBuilder();
									stringbuf.append("Line: " + astRoot.getLineNumber(node.getStartPosition()) + "\n");
									stringbuf.append("Start Position: " + node.getStartPosition() + "\n");
									stringbuf.append("Length: " + node.getLength() + "\n");
									stringbuf.append("Current source: " + cur+"\n");
									node.setOperator(InfixExpression.Operator.TIMES);
									stringbuf.append("New source: " + node + "\n");
									stringbuf.append("\n");
									mutants.add(stringbuf.toString());

									stringbuf = new StringBuilder();
									stringbuf.append("Line: " + astRoot.getLineNumber(node.getStartPosition()) + "\n");
									stringbuf.append("Start Position: " + node.getStartPosition() + "\n");
									stringbuf.append("Length: " + node.getLength() + "\n");
									stringbuf.append("Current source: " + cur+"\n");
									node.setOperator(InfixExpression.Operator.DIVIDE);
									stringbuf.append("New source: " + node + "\n");
									stringbuf.append("\n");
									mutants.add(stringbuf.toString());
									return true;
								}

								// and -> or operator
								else if(node.getOperator().toString().equals(InfixExpression.Operator.AND.toString())){
									StringBuilder stringbuf = new StringBuilder();
									stringbuf.append("Line: " + astRoot.getLineNumber(node.getStartPosition()) + "\n");
									stringbuf.append("Start Position: " + node.getStartPosition() + "\n");
									stringbuf.append("Length: " + node.getLength() + "\n");
									stringbuf.append("Current source: " + cur +"\n");
									node.setOperator(InfixExpression.Operator.OR);
									stringbuf.append("New source: " + node + "\n");
									stringbuf.append("\n");
									mutants.add(stringbuf.toString());
								}

								// or -> and operator
								else if(node.getOperator().toString().equals(InfixExpression.Operator.OR.toString())){
									StringBuilder stringbuf = new StringBuilder();
									stringbuf.append("Line: " + astRoot.getLineNumber(node.getStartPosition()) + "\n");
									stringbuf.append("Start Position: " + node.getStartPosition() + "\n");
									stringbuf.append("Length: " + node.getLength() + "\n");
									stringbuf.append("Current source: " + cur +"\n");
									node.setOperator(InfixExpression.Operator.AND);
									stringbuf.append("New source: " + node + "\n");
									stringbuf.append("\n");
									mutants.add(stringbuf.toString());
								}

								// cond and -> cond or operator
								else if(node.getOperator().toString().equals(InfixExpression.Operator.CONDITIONAL_AND.toString())){
									StringBuilder stringbuf = new StringBuilder();
									stringbuf.append("Line: " + astRoot.getLineNumber(node.getStartPosition()) + "\n");
									stringbuf.append("Start Position: " + node.getStartPosition() + "\n");
									stringbuf.append("Length: " + node.getLength() + "\n");
									stringbuf.append("Current source: " + cur +"\n");
									node.setOperator(InfixExpression.Operator.CONDITIONAL_OR);
									stringbuf.append("New source: " + node + "\n");
									stringbuf.append("\n");
									mutants.add(stringbuf.toString());
								}

								// cond or -> cond and operator
								else if(node.getOperator().toString().equals(InfixExpression.Operator.CONDITIONAL_OR.toString())){
									StringBuilder stringbuf = new StringBuilder();
									stringbuf.append("Line: " + astRoot.getLineNumber(node.getStartPosition()) + "\n");
									stringbuf.append("Start Position: " + node.getStartPosition() + "\n");
									stringbuf.append("Length: " + node.getLength() + "\n");
									stringbuf.append("Current source: " + cur +"\n");
									node.setOperator(InfixExpression.Operator.CONDITIONAL_AND);
									stringbuf.append("New source: " + node + "\n");
									stringbuf.append("\n");
									mutants.add(stringbuf.toString());
								}

								// > to >=
								else if(node.getOperator().toString().equals(InfixExpression.Operator.GREATER.toString())){
									StringBuilder stringbuf = new StringBuilder();
									stringbuf.append("Line: " + astRoot.getLineNumber(node.getStartPosition()) + "\n");
									stringbuf.append("Start Position: " + node.getStartPosition() + "\n");
									stringbuf.append("Length: " + node.getLength() + "\n");
									stringbuf.append("Current source: " + cur +"\n");
									node.setOperator(InfixExpression.Operator.GREATER_EQUALS);
									stringbuf.append("New source: " + node + "\n");
									stringbuf.append("\n");
									mutants.add(stringbuf.toString());
								}

								// < to <=
								else if(node.getOperator().toString().equals(InfixExpression.Operator.LESS.toString())){
									StringBuilder stringbuf = new StringBuilder();
									stringbuf.append("Line: " + astRoot.getLineNumber(node.getStartPosition()) + "\n");
									stringbuf.append("Start Position: " + node.getStartPosition() + "\n");
									stringbuf.append("Length: " + node.getLength() + "\n");
									stringbuf.append("Current source: " + cur +"\n");
									node.setOperator(InfixExpression.Operator.LESS_EQUALS);
									stringbuf.append("New source: " + node + "\n");
									stringbuf.append("\n");
									mutants.add(stringbuf.toString());
								}


								//multiply operator
								else if(node.getOperator().toString().equals("*")){
									StringBuilder stringbuf = new StringBuilder();
									stringbuf.append("Line: " + astRoot.getLineNumber(node.getStartPosition()) + "\n");
									stringbuf.append("Start Position: " + node.getStartPosition() + "\n");
									stringbuf.append("Length: " + node.getLength() + "\n");
									stringbuf.append("Current source: " + cur +"\n");
									node.setOperator(InfixExpression.Operator.PLUS);
									stringbuf.append("New source: " + node + "\n");
									stringbuf.append("\n");
									mutants.add(stringbuf.toString());

									stringbuf = new StringBuilder();
									stringbuf.append("Line: " + astRoot.getLineNumber(node.getStartPosition()) + "\n");
									stringbuf.append("Start Position: " + node.getStartPosition() + "\n");
									stringbuf.append("Length: " + node.getLength() + "\n");
									stringbuf.append("Current source: " + cur+"\n");
									node.setOperator(InfixExpression.Operator.MINUS);
									stringbuf.append("New source: " + node + "\n");
									stringbuf.append("\n");
									mutants.add(stringbuf.toString());

									stringbuf = new StringBuilder();
									stringbuf.append("Line: " + astRoot.getLineNumber(node.getStartPosition()) + "\n");
									stringbuf.append("Start Position: " + node.getStartPosition() + "\n");
									stringbuf.append("Length: " + node.getLength() + "\n");
									stringbuf.append("Current source: " + cur+"\n");
									node.setOperator(InfixExpression.Operator.DIVIDE);
									stringbuf.append("New source: " + node + "\n");
									stringbuf.append("\n");
									mutants.add(stringbuf.toString());
									return true;
								}

								//Divide operator
								else if(node.getOperator().toString().equals("/")){
									StringBuilder stringbuf = new StringBuilder();
									stringbuf.append("Line: " + astRoot.getLineNumber(node.getStartPosition()) + "\n");
									stringbuf.append("Start Position: " + node.getStartPosition() + "\n");
									stringbuf.append("Length: " + node.getLength() + "\n");
									stringbuf.append("Current source: " + cur +"\n");
									node.setOperator(InfixExpression.Operator.PLUS);
									stringbuf.append("New source: " + node + "\n");
									stringbuf.append("\n");
									mutants.add(stringbuf.toString());

									stringbuf = new StringBuilder();
									stringbuf.append("Line: " + astRoot.getLineNumber(node.getStartPosition()) + "\n");
									stringbuf.append("Start Position: " + node.getStartPosition() + "\n");
									stringbuf.append("Length: " + node.getLength() + "\n");
									stringbuf.append("Current source: " + cur+"\n");
									node.setOperator(InfixExpression.Operator.MINUS);
									stringbuf.append("New source: " + node + "\n");
									stringbuf.append("\n");
									mutants.add(stringbuf.toString());

									stringbuf = new StringBuilder();
									stringbuf.append("Line: " + astRoot.getLineNumber(node.getStartPosition()) + "\n");
									stringbuf.append("Start Position: " + node.getStartPosition() + "\n");
									stringbuf.append("Length: " + node.getLength() + "\n");
									stringbuf.append("Current source: " + cur+"\n");
									node.setOperator(InfixExpression.Operator.TIMES);
									stringbuf.append("New source: " + node + "\n");
									stringbuf.append("\n");
									mutants.add(stringbuf.toString());

									return true;
								}
								// % operator
								else if(node.getOperator().toString().equals("%")){
									StringBuilder stringbuf = new StringBuilder();
									stringbuf.append("Line: " + astRoot.getLineNumber(node.getStartPosition()) + "\n");
									stringbuf.append("Start Position: " + node.getStartPosition() + "\n");
									stringbuf.append("Length: " + node.getLength() + "\n");
									stringbuf.append("Current source: " + cur +"\n");
									node.setOperator(InfixExpression.Operator.DIVIDE);
									stringbuf.append("New source: " + node + "\n");
									stringbuf.append("\n");
									mutants.add(stringbuf.toString());

									stringbuf = new StringBuilder();
									stringbuf.append("Line: " + astRoot.getLineNumber(node.getStartPosition()) + "\n");
									stringbuf.append("Start Position: " + node.getStartPosition() + "\n");
									stringbuf.append("Length: " + node.getLength() + "\n");
									stringbuf.append("Current source: " + cur+"\n");
									node.setOperator(InfixExpression.Operator.MINUS);
									stringbuf.append("New source: " + node + "\n");
									stringbuf.append("\n");
									mutants.add(stringbuf.toString());

									stringbuf = new StringBuilder();
									stringbuf.append("Line: " + astRoot.getLineNumber(node.getStartPosition()) + "\n");
									stringbuf.append("Start Position: " + node.getStartPosition() + "\n");
									stringbuf.append("Length: " + node.getLength() + "\n");
									stringbuf.append("Current source: " + cur+"\n");
									node.setOperator(InfixExpression.Operator.TIMES);
									stringbuf.append("New source: " + node + "\n");
									stringbuf.append("\n");
									mutants.add(stringbuf.toString());

									stringbuf = new StringBuilder();
									stringbuf.append("Line: " + astRoot.getLineNumber(node.getStartPosition()) + "\n");
									stringbuf.append("Start Position: " + node.getStartPosition() + "\n");
									stringbuf.append("Length: " + node.getLength() + "\n");
									stringbuf.append("Current source: " + cur +"\n");
									node.setOperator(InfixExpression.Operator.PLUS);
									stringbuf.append("New source: " + node + "\n");
									stringbuf.append("\n");
									mutants.add(stringbuf.toString());

									return true;
								}
								// ++ operator
								else if(node.getOperator().toString().equals("++")){
									//node.getOP
									StringBuilder stringbuf = new StringBuilder();
									stringbuf.append("Line: " + astRoot.getLineNumber(node.getStartPosition()) + "\n");
									stringbuf.append("Start Position: " + node.getStartPosition() + "\n");
									stringbuf.append("Length: " + node.getLength() + "\n");
									stringbuf.append("Current source: " + cur +"\n");
									//node.
									node.setOperator(InfixExpression.Operator.MINUS);
									stringbuf.append("New source: " + node + "\n");
									stringbuf.append("\n");
									mutants.add(stringbuf.toString());

									return true;
								}

								return true;
							}
						});
					}
				}
			}
			//}

			//Log mutants to a file
			PrintWriter writer = new PrintWriter(new File(mutationPlanPath));
			for(String mutant : mutants)
			{
				writer.print(mutant);    			  
			}

			writer.close();
		} 
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return mutants;
	}

	private static CompilationUnit parseStringToCompilationUnit(String unit) 
	{
		@SuppressWarnings("deprecation")
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(unit.toCharArray());
		parser.setResolveBindings(true);
		return (CompilationUnit) parser.createAST(null);
	}

}
