package org.eclipse.jdt.internal.compiler.ast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;


import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.codegen.CodeStream;
import org.eclipse.jdt.internal.compiler.codegen.Opcodes;
import org.eclipse.jdt.internal.compiler.flow.ExceptionHandlingFlowContext;
import org.eclipse.jdt.internal.compiler.flow.FlowInfo;
import org.eclipse.jdt.internal.compiler.lookup.ArrayBinding;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.compiler.lookup.PackageBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemMethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeIds;

public class FormalSpecification {

	private static final char[] preconditionAssertionMessage = "Precondition does not hold".toCharArray(); //$NON-NLS-1$
	private static final char[] postconditionAssertionMessage = "Postcondition does not hold".toCharArray(); //$NON-NLS-1$
	private static final char[] throwsAssertionMessage = "@throws condition holds but specified exception type not thrown".toCharArray(); //$NON-NLS-1$
	private static final char[] thrownExceptionNotformal = "The thrown exception was not specified in the formal specification".toCharArray(); //$NON-NLS-1$
	private static final char[] POSTCONDITION_VARIABLE_NAME = " $post".toCharArray(); //$NON-NLS-1$
	private static final char[] PRECONDITION_METHOD_NAME_SUFFIX = "$pre".toCharArray(); //$NON-NLS-1$
	private static final char[] POSTCONDITION_METHOD_NAME_SUFFIX = "$post".toCharArray(); //$NON-NLS-1$
	public static final char[] SPECIFICATION_METHOD_NAME_SUFFIX = "$spec".toCharArray(); //$NON-NLS-1$
	static final char[] OLD_VARIABLE_INNER_SUFFIX = " inner".toCharArray(); //$NON-NLS-1$
	static final char[] OLD_VARIABLE_EXCEPTION_SUFFIX = " exception".toCharArray(); //$NON-NLS-1$
	private static final char[] LAMBDA_PARAMETER_NAME = " $result".toCharArray(); //$NON-NLS-1$
	private static final char[] LAMBDA_PARAMETER2_NAME = " $exception".toCharArray(); //$NON-NLS-1$
	private static final char[] RESULT_NAME = "result".toCharArray(); //$NON-NLS-1$
	private static final char[] A_THROW_CLAUSE_SATISFIED_AND_NOT_THROWN_VARIABLE_NAME = "$aThrowClauseSatisfiedAndNotThrown".toCharArray(); //$NON-NLS-1$
	private static final char[] GETLOGGER_METHOD_VARIABLE_NAME = "$getLoggerMethod".toCharArray(); //$NON-NLS-1$
	private static final char[] SEVERE_METHOD_VARIABLE_NAME= "$severeMethod".toCharArray(); //$NON-NLS-1$

	
	private static QualifiedTypeReference getTypeReference(String name) {
		String[] components = name.split("\\."); //$NON-NLS-1$
		char[][] sources = new char[components.length][];
		long[] poss = new long[components.length];
		for (int i = 0; i < components.length; i++)
			sources[i] = components[i].toCharArray();
		return new QualifiedTypeReference(sources, poss);
	}
	
	private static QualifiedNameReference getNameReference(String name) {
		String[] components = name.split("\\."); //$NON-NLS-1$
		char[][] tokens = new char[components.length][];
		long[] positions = new long[components.length];
		for (int i = 0; i < components.length; i++)
			tokens[i] = components[i].toCharArray();
		return new QualifiedNameReference(tokens, positions, 0, 0);
	}
	
	private static QualifiedTypeReference javaLangObject() { return getTypeReference("java.lang.Object"); } //$NON-NLS-1$
	private static QualifiedNameReference javaLangClassNameReference() { return getNameReference("java.lang.Class"); } //$NON-NLS-1$
	private static QualifiedTypeReference javaLangString() { return getTypeReference("java.lang.String"); } //$NON-NLS-1$
	private static QualifiedTypeReference javaLangReflectMethod() { return getTypeReference("java.lang.reflect.Method"); } //$NON-NLS-1$
	private static QualifiedTypeReference javaLangThrowable() { return getTypeReference("java.lang.Throwable"); } //$NON-NLS-1$
	private static QualifiedTypeReference javaLangRuntimeException() { return getTypeReference("java.lang.RuntimeException"); } //$NON-NLS-1$
	private static QualifiedTypeReference javaLangAssertionError() { return getTypeReference("java.lang.AssertionError"); } //$NON-NLS-1$
	private static QualifiedTypeReference javaLangException() { return getTypeReference("java.lang.Exception"); } //$NON-NLS-1$
	private static QualifiedTypeReference javaLangRunnable() { return getTypeReference("java.lang.Runnable"); } //$NON-NLS-1$
	private static QualifiedTypeReference javaUtilFunctionConsumer() { return getTypeReference("java.util.function.Consumer"); } //$NON-NLS-1$
	private static QualifiedTypeReference javaUtilFunctionBiConsumer() { return getTypeReference("java.util.function.BiConsumer"); } //$NON-NLS-1$
	private static QualifiedTypeReference javaUtilFunctionSupplier() { return getTypeReference("java.util.function.Supplier"); } //$NON-NLS-1$

	private static TypeReference getBoxedType(TypeBinding binding, TypeReference reference) {
		switch (binding.id) {
			case TypeIds.T_boolean: return getTypeReference("java.lang.Boolean"); //$NON-NLS-1$
			case TypeIds.T_byte: return getTypeReference("java.lang.Byte"); //$NON-NLS-1$
			case TypeIds.T_char: return getTypeReference("java.lang.Character"); //$NON-NLS-1$
			case TypeIds.T_double: return getTypeReference("java.lang.Double"); //$NON-NLS-1$
			case TypeIds.T_float: return getTypeReference("java.lang.Float"); //$NON-NLS-1$
			case TypeIds.T_int: return getTypeReference("java.lang.Integer"); //$NON-NLS-1$
			case TypeIds.T_long: return getTypeReference("java.lang.Long"); //$NON-NLS-1$
			case TypeIds.T_short: return getTypeReference("java.lang.Short"); //$NON-NLS-1$
			default: return reference;
		}
	}
	
	private static QualifiedTypeReference getJavaUtilConsumerOf(TypeReference typeArgument) {
		TypeReference[][] typeArguments = new TypeReference[][] { null, null, null, {typeArgument}};
		QualifiedTypeReference javaUtilFunctionConsumer = javaUtilFunctionConsumer();
		return new ParameterizedQualifiedTypeReference(javaUtilFunctionConsumer.tokens, typeArguments, 0, javaUtilFunctionConsumer.sourcePositions);
	}
	
	private static QualifiedTypeReference getJavaUtilBiConsumerOf(TypeReference typeArgument1, TypeReference typeArgument2) {
		TypeReference[][] typeArguments = new TypeReference[][] { null, null, null, {typeArgument1, typeArgument2}};
		QualifiedTypeReference javaUtilFunctionBiConsumer = javaUtilFunctionBiConsumer();
		return new ParameterizedQualifiedTypeReference(javaUtilFunctionBiConsumer.tokens, typeArguments, 0, javaUtilFunctionBiConsumer.sourcePositions);
	}

	private static QualifiedTypeReference getJavaUtilSupplierOf(TypeReference typeArgument) {
		TypeReference[][] typeArguments = new TypeReference[][] { null, null, null, {typeArgument}};
		QualifiedTypeReference javaUtilFunctionSupplier = javaUtilFunctionSupplier();
		return new ParameterizedQualifiedTypeReference(javaUtilFunctionSupplier.tokens, typeArguments, 0, javaUtilFunctionSupplier.sourcePositions);
	}

	private static QualifiedTypeReference getPostconditionLambdaType(TypeBinding returnTypeBinding, TypeReference returnType) {
		if (returnType == null) // constructor
			return getJavaUtilBiConsumerOf(javaLangObject(), javaLangRuntimeException());
		switch (returnTypeBinding.id) {
			case TypeIds.T_void: return getJavaUtilConsumerOf(javaLangRuntimeException());
			default: return getJavaUtilBiConsumerOf(getBoxedType(returnTypeBinding, returnType), javaLangRuntimeException());
		}
	}

	public final AbstractMethodDeclaration method;
	public Expression[] invariants; // Package representation invariants are specified in the Javadoc comments for the default access getters.
	public Expression[] preconditions;
	public TypeReference[] throwsExceptionTypeNames;
	public Expression[] throwsConditions;
	public TypeReference[] mayThrowExceptionTypeNames;
	public Expression[] mayThrowConditions;
	public Expression[] postconditions;
	
	// All of the below are null if no corresponding Javadoc tag is present; they are an empty array if an empty tag is present.
	public Expression[] inspectsExpressions;
	public Expression[] mutatesExpressions;
	public Expression[] mutatesPropertiesExpressions;
	public Expression[] createsExpressions;
	
	public LambdaExpression preconditionLambda;
	public Block block;
	public LocalDeclaration postconditionVariableDeclaration;
	public MessageSend postconditionMethodCall;
	public ArrayList<Statement> statementsForMethodBody;

	public FormalSpecification(AbstractMethodDeclaration method) {
		this.method = method;
	}

	public void print(int tab, StringBuffer output) {
		if (this.invariants != null) {
			for (int i = 0; i < this.invariants.length; i++) {
				output.append("/** @invar | "); //$NON-NLS-1$
				this.invariants[i].printExpression(tab, output);
				output.append(" */"); //$NON-NLS-1$
			}
		}
		if (this.preconditions != null) {
			for (int i = 0; i < this.preconditions.length; i++) {
				output.append("/** @pre | "); //$NON-NLS-1$
				this.preconditions[i].printExpression(tab, output);
				output.append(" */"); //$NON-NLS-1$
			}
		}
		if (this.throwsConditions != null) {
			for (int i = 0; i < this.throwsConditions.length; i++) {
				output.append("/** @throws | "); //$NON-NLS-1$
				this.throwsConditions[i].printExpression(tab, output);
				output.append(" */"); //$NON-NLS-1$
			}
		}
		if (this.mayThrowConditions != null) {
			for (int i = 0; i < this.mayThrowConditions.length; i++) {
				output.append("/** @may_throw | "); //$NON-NLS-1$
				this.mayThrowConditions[i].printExpression(tab, output);
				output.append(" */"); //$NON-NLS-1$
			}
		}
		if (this.postconditions != null) {
			for (int i = 0; i < this.postconditions.length; i++) {
				output.append("/** @post | "); //$NON-NLS-1$
				this.postconditions[i].printExpression(tab, output);
				output.append(" */"); //$NON-NLS-1$
			}
		}
		if (this.inspectsExpressions != null) {
			output.append("/** @inspects | "); //$NON-NLS-1$
			for (int i = 0; i < this.inspectsExpressions.length; i++) {
				if (i != 0)
					output.append(", "); //$NON-NLS-1$
				this.inspectsExpressions[i].printExpression(tab, output);
			}
			output.append(" */"); //$NON-NLS-1$
		}
		if (this.mutatesExpressions != null) {
			output.append("/** @mutates | "); //$NON-NLS-1$
			for (int i = 0; i < this.mutatesExpressions.length; i++) {
				if (i != 0)
					output.append(", "); //$NON-NLS-1$
				this.mutatesExpressions[i].printExpression(tab, output);
			}
			output.append(" */"); //$NON-NLS-1$
		}
		if (this.mutatesPropertiesExpressions != null) {
			output.append("/** @mutates_properties | "); //$NON-NLS-1$
			for (int i = 0; i < this.mutatesPropertiesExpressions.length; i++) {
				if (i != 0)
					output.append(", "); //$NON-NLS-1$
				this.mutatesPropertiesExpressions[i].printExpression(tab, output);
			}
			output.append(" */"); //$NON-NLS-1$
		}
		if (this.createsExpressions != null) {
			output.append("/** @creates | "); //$NON-NLS-1$
			for (int i = 0; i < this.createsExpressions.length; i++) {
				if (i != 0)
					output.append(", "); //$NON-NLS-1$
				this.createsExpressions[i].printExpression(tab, output);
			}
			output.append(" */"); //$NON-NLS-1$
		}
	}
	
	private void resolveEffectClause(Expression[] expressions) {
		TypeBinding javaLangObject = this.method.scope.getJavaLangObject();
		
		if (expressions != null) {
			for (Expression e : expressions) {
				if (e instanceof SpreadExpression)
					((SpreadExpression)e).body.resolveTypeExpecting(this.method.scope, javaLangObject); // TODO: Should be Iterable<?>
				else
					e.resolveTypeExpecting(this.method.scope, javaLangObject);
			}
		}
	}
	
	public void initializeMethodBinding() {
		
		if ((this.method.modifiers & (ClassFileConstants.AccStatic | ClassFileConstants.AccPrivate | ClassFileConstants.AccFinal)) == 0
				&& !this.method.binding.declaringClass.isFinal() && (this.preconditions != null || this.postconditions != null || this.throwsConditions != null || this.mayThrowConditions != null)) {

			this.method.binding.hasSpecificationMethod = true;
		    
		}

	}
	
	public void resolve() {
		if (this.method.ignoreFurtherInvestigation)
			return;
		
		ArrayList<Statement> statementsForBlock = new ArrayList<>();
		HashMap<String, OldExpression.DistinctExpression> oldExpressions = new HashMap<>();
		int blockDeclarationsCount = 0;
		this.statementsForMethodBody = new ArrayList<>();
			
		resolveEffectClause(this.inspectsExpressions);
		resolveEffectClause(this.mutatesExpressions);
		//resolveEffectClause(this.createsExpressions); // @creates expressions can refer to 'result'
		
		if (this.mutatesPropertiesExpressions != null) {
			for (Expression e : this.mutatesPropertiesExpressions) {
				if (!(e instanceof MessageSend))
					this.method.scope.problemReporter().mutatesPropertiesExpressionShouldBeMethodCall(e);
				else {
					MessageSend messageSend = (MessageSend)e;
					if (messageSend.arguments != null && messageSend.arguments.length != 0)
						this.method.scope.problemReporter().mutatesPropertiesMethodCallShouldNotSpecifyArguments(e);
					else if (messageSend.receiver instanceof SpreadExpression) {
						SpreadExpression receiver = (SpreadExpression)messageSend.receiver;
						char[] elementName = "spread$element".toCharArray(); //$NON-NLS-1$
						LocalDeclaration elementVariable = new LocalDeclaration(elementName, e.sourceStart, e.sourceEnd);
						long pos = ((long)e.sourceStart << 32) + e.sourceEnd;
						elementVariable.type = new SingleTypeReference("var".toCharArray(), pos); //$NON-NLS-1$
						elementVariable.bits |= ASTNode.IsForeachElementVariable;
						ForeachStatement s = new ForeachStatement(elementVariable, e.sourceStart);
						s.collection = receiver.body;
						messageSend.receiver = new SingleNameReference(elementName, pos);
						s.action = messageSend;
						s.resolve(this.method.scope);
						messageSend.receiver = receiver;
					} else {
						e.resolveType(this.method.scope);
					}
				}
			}
		}
		
		{
			int overloadCount = this.method.scope.enclosingClassScope().registerOverload(this.method.selector);
			MessageSend preconditionLambdaCall = new MessageSend();
			if (!(this.method instanceof ConstructorDeclaration)) {
				this.preconditionLambda = new LambdaExpression(this.method.compilationResult, false);
				this.preconditionLambda.allowReferencesToNonEffectivelyFinalOuterLocals = true;
				this.preconditionLambda.lambdaMethodSelector =
						overloadCount == 0 ?
								CharOperation.concat(this.method.selector, PRECONDITION_METHOD_NAME_SUFFIX)
						:
								CharOperation.concat(this.method.selector, ("$" + overloadCount).toCharArray(), PRECONDITION_METHOD_NAME_SUFFIX); //$NON-NLS-1$
				TypeReference preconditionLambdaType;
				if (this.postconditions == null && this.throwsConditions == null && this.mayThrowConditions == null) {
					preconditionLambdaType = javaLangRunnable();
					preconditionLambdaCall.selector = "run".toCharArray(); //$NON-NLS-1$
				} else {
					preconditionLambdaType = getJavaUtilSupplierOf(getPostconditionLambdaType(this.method.binding.returnType, this.method instanceof MethodDeclaration ? ((MethodDeclaration)this.method).returnType : null));
					preconditionLambdaCall.selector = "get".toCharArray(); //$NON-NLS-1$
				}
				preconditionLambdaCall.receiver = new CastExpression(this.preconditionLambda, preconditionLambdaType);
			}

			
			if (this.preconditions != null) {
				// Insert assert statements into method body.
				for (int i = 0; i < this.preconditions.length; i++) {
					Expression e = this.preconditions[i];
					statementsForBlock.add(new AssertStatement(new StringLiteral(preconditionAssertionMessage, e.sourceStart, e.sourceEnd, 0), e, e.sourceStart));
				}
			}
			if (this.postconditions != null || this.throwsConditions != null || this.mayThrowConditions != null) {
				this.postconditionMethodCall = new MessageSend();
				this.postconditionMethodCall.receiver = new SingleNameReference(POSTCONDITION_VARIABLE_NAME, (this.method.bodyStart<< 32) + this.method.bodyStart);
				this.postconditionMethodCall.selector = "accept".toCharArray(); //$NON-NLS-1$
				if (this.method.binding.returnType.id == TypeIds.T_void && !(this.method instanceof ConstructorDeclaration))
					if (this.throwsConditions == null && this.mayThrowConditions == null)
						this.postconditionMethodCall.arguments = new Expression[] {new NullLiteral(0, 0)};
					else
						this.postconditionMethodCall.arguments = new Expression[] {new SingleNameReference(LAMBDA_PARAMETER2_NAME, 0)};
				else
					this.postconditionMethodCall.arguments = new Expression[] {new NullLiteral(0, 0), new NullLiteral(0, 0)};
				int postconditionsLength = this.postconditions == null ? 0 : this.postconditions.length;
				for (int i = 0; i < postconditionsLength; i++) {
					ASTVisitor astVisitor = this.generateOldExpressionASTVisitor(oldExpressions, statementsForBlock);
					this.postconditions[i].traverse(astVisitor, this.method.scope);
				}
				//blockDeclarationsCount += 2 * oldExpressions.size();
				
				ArrayList<Statement> postconditionBlockStatements = new ArrayList<>();
				int postconditionBlockDeclarationsCount = 0;
				
				LocalDeclaration getLoggerMethodVariableDeclaration = new LocalDeclaration(GETLOGGER_METHOD_VARIABLE_NAME, this.method.sourceStart, this.method.sourceEnd);
				getLoggerMethodVariableDeclaration.type = javaLangReflectMethod();
				
				LocalDeclaration severeMethodVariableDeclaration = new LocalDeclaration(SEVERE_METHOD_VARIABLE_NAME, this.method.sourceStart, this.method.sourceEnd);
				severeMethodVariableDeclaration.type = javaLangReflectMethod();
				
				postconditionBlockStatements.add(getLoggerMethodVariableDeclaration);
				postconditionBlockStatements.add(severeMethodVariableDeclaration);
				blockDeclarationsCount += 2;
				
				if (this.throwsConditions != null || this.mayThrowConditions != null) {
					Block body = new Block(this.method.explicitDeclarations);
					body.statements = this.method.statements;
					ArrayList<Statement> statementsForOuterBlock = new ArrayList<>();
					
					Statement[] catchBlockStatements = new Statement[] {
							this.postconditionMethodCall,
							new ThrowStatement(new SingleNameReference(LAMBDA_PARAMETER2_NAME, (this.method.sourceStart << 32) + this.method.sourceStart), this.method.sourceStart, this.method.sourceEnd)
					};
					TryStatement tryMethodStatement = generateTryCatchBlock(body, LAMBDA_PARAMETER2_NAME, javaLangRuntimeException(), catchBlockStatements, this.method.sourceStart, this.method.sourceEnd);
					statementsForOuterBlock.add(tryMethodStatement);
					
					Block outerBlock = new Block(1);
					outerBlock.statements = statementsForOuterBlock.toArray(new Statement[statementsForOuterBlock.size()]);
					
					this.method.statements = new Statement[] {outerBlock};
					this.method.explicitDeclarations = 0;	
				}
				if (this.mayThrowConditions != null) {
					for (int i = 0 ; i < this.mayThrowConditions.length ; i++) {
						if (this.mayThrowExceptionTypeNames[i] == null) {
							continue;
						}
						Expression e = this.mayThrowConditions[i];
						
						this.mayThrowConditions[i] = new OldExpression(e.sourceStart, e, e.sourceEnd, this.method.compilationResult.compilationUnit.getContents());
						ASTVisitor astVisitor = this.generateOldExpressionASTVisitor(oldExpressions, statementsForBlock);
						this.mayThrowConditions[i].traverse(astVisitor, this.method.scope);
					}
					//blockDeclarationsCount += 2 * oldExpressions.size();
					
					Statement statement = new EmptyStatement(0,0);
					for (int i = 0; i < this.mayThrowConditions.length; i++) {
						if (this.mayThrowExceptionTypeNames[i] == null) {
							continue;
						}
						Expression e = this.mayThrowConditions[i];
						TryStatement loggerMessage = generateSevereLoggerMessageIfPossible(thrownExceptionNotformal, e.sourceStart, e.sourceEnd);
						Expression condition = new InstanceOfExpression(
								new SingleNameReference(LAMBDA_PARAMETER2_NAME, (this.method.bodyStart << 32) + this.method.bodyStart),
								this.mayThrowExceptionTypeNames[i]
						);
						Block noMayThrowConditionsSatisfiedBlock = new Block(0);
						noMayThrowConditionsSatisfiedBlock.statements = new Statement[] {loggerMessage,
								new ThrowStatement(new SingleNameReference(LAMBDA_PARAMETER2_NAME, (e.sourceStart << 32) + e.sourceStart), e.sourceStart, e.sourceEnd)
						};
						
						Block thenBlock = new Block(0);
						thenBlock.statements = new Statement[] {
							new IfStatement(e, new ThrowStatement(new SingleNameReference(LAMBDA_PARAMETER2_NAME, (this.method.bodyStart << 32) + this.method.bodyStart), e.sourceStart, e.sourceEnd), noMayThrowConditionsSatisfiedBlock, e.sourceStart, e.sourceEnd)
						};
						statement = new IfStatement(condition, thenBlock, statement, e.sourceStart, e.sourceEnd);
					}	
					postconditionBlockStatements.add(statement);
				}	
				if (this.throwsConditions != null) {
					LocalDeclaration aThrowClauseSatisfiedAndNotThrownVariableDeclaration = new LocalDeclaration(A_THROW_CLAUSE_SATISFIED_AND_NOT_THROWN_VARIABLE_NAME, this.method.bodyStart, this.method.bodyStart);
					aThrowClauseSatisfiedAndNotThrownVariableDeclaration.type = TypeReference.baseTypeReference(TypeIds.T_boolean, 0);
					aThrowClauseSatisfiedAndNotThrownVariableDeclaration.initialization = new FalseLiteral(0,0);
					
					postconditionBlockStatements.add(aThrowClauseSatisfiedAndNotThrownVariableDeclaration);
					blockDeclarationsCount += 1;
					
					for (int i = 0 ; i < this.throwsConditions.length ; i++) {
						if (this.throwsExceptionTypeNames[i] == null) {
							continue;
						}
						Expression e = this.throwsConditions[i];
						
						this.throwsConditions[i] = new OldExpression(e.sourceStart, e, e.sourceEnd, this.method.compilationResult.compilationUnit.getContents());
						ASTVisitor astVisitor = this.generateOldExpressionASTVisitor(oldExpressions, statementsForBlock);
						this.throwsConditions[i].traverse(astVisitor, this.method.scope);
					}
					//blockDeclarationsCount += 2 * oldExpressions.size();

					for (int i = 0; i < this.throwsConditions.length; i++) {
						Expression e = this.throwsConditions[i];

						if (this.throwsExceptionTypeNames[i] == null)
							continue;
						if (e instanceof FalseLiteral)
							continue;

						Expression instanceOfExpression = new InstanceOfExpression(
								new SingleNameReference(LAMBDA_PARAMETER2_NAME, (this.method.bodyStart << 32) + this.method.bodyStart),
								this.throwsExceptionTypeNames[i]
						);
						Assignment aThrowClauseSatisfiedAndNotThrownVariableAssigmentTrue = new Assignment(new SingleNameReference(A_THROW_CLAUSE_SATISFIED_AND_NOT_THROWN_VARIABLE_NAME, (e.sourceStart << 32) | e.sourceEnd), new TrueLiteral(0,0), e.sourceEnd);
						Expression exceptionNotNullExpression = new EqualExpression(
								new SingleNameReference(LAMBDA_PARAMETER2_NAME, (e.sourceStart << 32) | e.sourceEnd),
								new NullLiteral(0, 0),
								OperatorIds.NOT_EQUAL);
						IfStatement exceptionNotNullIfStatement = new IfStatement(
								exceptionNotNullExpression,
								generateSevereLoggerMessageIfPossible(throwsAssertionMessage, e.sourceStart, e.sourceEnd),
								e.sourceStart,
								e.sourceEnd);

						Block instanceOfThenBlock = new Block(0);
						instanceOfThenBlock.statements = new Statement[] {
								new ThrowStatement(new SingleNameReference(LAMBDA_PARAMETER2_NAME, 0), this.method.bodyStart, this.method.bodyEnd),
						};
						Block instanceOfElseBlock = new Block(0);
						instanceOfElseBlock.statements = new Statement[] {
								aThrowClauseSatisfiedAndNotThrownVariableAssigmentTrue,
								exceptionNotNullIfStatement
						};
						Block throwConditionThenBlock = new Block(0);
						throwConditionThenBlock.statements = new Statement[] {
								new IfStatement(
										instanceOfExpression, 
										instanceOfThenBlock,
										instanceOfElseBlock,
										e.sourceStart, 
										e.sourceEnd)
						};
						
						IfStatement clauseSatisfiedIfStatement = new IfStatement(e, throwConditionThenBlock, e.sourceStart, e.sourceEnd);
						postconditionBlockStatements.add(clauseSatisfiedIfStatement);
					}
					
					AllocationExpression assertionError = new AllocationExpression();
					assertionError.type = javaLangAssertionError();
					assertionError.arguments = new Expression[] {
							new StringLiteral(throwsAssertionMessage, this.method.sourceStart, this.method.sourceEnd, 0),
					};
					assertionError.sourceStart = this.method.sourceStart;
					assertionError.sourceEnd = this.method.sourceEnd;
					
					Expression exceptionNullExpression = new EqualExpression(
								new SingleNameReference(LAMBDA_PARAMETER2_NAME, (this.method.sourceStart << 32) | this.method.sourceEnd),
								new NullLiteral(0, 0),
								OperatorIds.EQUAL_EQUAL);
					
					IfStatement aThrowClauseSatisfiedAndNotThrownIfStatement = new IfStatement(
							new AND_AND_Expression(new SingleNameReference(A_THROW_CLAUSE_SATISFIED_AND_NOT_THROWN_VARIABLE_NAME, (this.method.sourceStart << 32) | this.method.sourceEnd), exceptionNullExpression, OperatorIds.AND_AND),
							new ThrowStatement(assertionError, this.method.sourceStart, this.method.sourceEnd),
							this.method.sourceStart,
							this.method.sourceEnd);
					postconditionBlockStatements.add(aThrowClauseSatisfiedAndNotThrownIfStatement);

				}
				{
					Expression condition = new InstanceOfExpression(
							new SingleNameReference(LAMBDA_PARAMETER2_NAME, (this.method.bodyStart << 32) + this.method.bodyStart),
							javaLangRuntimeException());
					Block thenBlock = new Block(0);
					thenBlock.statements = new Statement[]{
							generateSevereLoggerMessageIfPossible(thrownExceptionNotformal, this.method.sourceStart, this.method.sourceEnd),
							new ThrowStatement(new SingleNameReference(LAMBDA_PARAMETER2_NAME, 0), this.method.bodyStart, this.method.bodyEnd)};
					postconditionBlockStatements.add(new IfStatement(condition, thenBlock, this.method.bodyStart, this.method.bodyStart));
				}
				
				LocalDeclaration resultDeclaration = null;
				if (this.method instanceof MethodDeclaration) {
					MethodDeclaration md = (MethodDeclaration)this.method;
					if (md.binding.returnType.id != TypeIds.T_void) {
						resultDeclaration = new LocalDeclaration(RESULT_NAME, this.method.bodyStart, this.method.bodyStart);
						resultDeclaration.type = md.returnType;
						resultDeclaration.initialization = new SingleNameReference(LAMBDA_PARAMETER_NAME, (this.method.bodyStart << 32) + this.method.bodyStart);
						postconditionBlockStatements.add(resultDeclaration);
						postconditionBlockDeclarationsCount++;
					}
				}
				for (int i = 0; i < postconditionsLength; i++) {
					Expression e = this.postconditions[i];
					postconditionBlockStatements.add(new AssertStatement(new StringLiteral(postconditionAssertionMessage, e.sourceStart, e.sourceEnd, 0), e, e.sourceStart));
				}		
				Block postconditionBlock = new Block(postconditionBlockDeclarationsCount);
				postconditionBlock.statements = new Statement[postconditionBlockStatements.size()];
				postconditionBlockStatements.toArray(postconditionBlock.statements);
				postconditionBlock.sourceStart = Math.min(
						this.postconditions == null ? Integer.MAX_VALUE : this.postconditions[0].sourceStart,
						this.throwsConditions == null ? Integer.MAX_VALUE : this.throwsConditions[0].sourceStart);
				postconditionBlock.sourceEnd = this.method.bodyEnd;
				LambdaExpression postconditionLambda = new LambdaExpression(this.method.compilationResult, false);
				postconditionLambda.allowReferencesToNonEffectivelyFinalOuterLocals = true;
				if (this.method instanceof ConstructorDeclaration)
					postconditionLambda.lateBindReceiver = true;
				postconditionLambda.lambdaMethodSelector =
						overloadCount == 0 ?
								CharOperation.concat(this.method.selector, POSTCONDITION_METHOD_NAME_SUFFIX)
						:
								CharOperation.concat(this.method.selector, ("$" + overloadCount).toCharArray(), POSTCONDITION_METHOD_NAME_SUFFIX); //$NON-NLS-1$
				if (this.method.binding.returnType.id != TypeIds.T_void || this.method instanceof ConstructorDeclaration)
					postconditionLambda.setArguments(new Argument[] {
							new Argument(LAMBDA_PARAMETER_NAME, (this.method.bodyStart << 32) + this.method.bodyStart, null, 0, true),
							new Argument(LAMBDA_PARAMETER2_NAME, (this.method.bodyStart << 32) + this.method.bodyStart, null, 0, true)
					});
				else
					postconditionLambda.setArguments(new Argument[] {
							new Argument(LAMBDA_PARAMETER2_NAME, (this.method.bodyStart << 32) + this.method.bodyStart, null, 0, true)
					});
				postconditionLambda.setBody(postconditionBlock);
				postconditionLambda.sourceStart = this.method.bodyStart;
				postconditionLambda.sourceEnd = this.method.bodyEnd;

				this.postconditionVariableDeclaration = new LocalDeclaration(POSTCONDITION_VARIABLE_NAME, this.method.bodyStart, this.method.bodyStart);
				this.postconditionVariableDeclaration.type = getPostconditionLambdaType(this.method.binding.returnType, this.method instanceof MethodDeclaration ? ((MethodDeclaration)this.method).returnType : null);
				this.statementsForMethodBody.add(this.postconditionVariableDeclaration);
				this.method.explicitDeclarations++;

				if (this.method instanceof ConstructorDeclaration)
					statementsForBlock.add(new Assignment(new SingleNameReference(this.postconditionVariableDeclaration.name, (this.method.bodyStart << 32) + this.method.bodyStart), postconditionLambda, this.method.bodyStart));
				else {
					statementsForBlock.add(new ReturnStatement(postconditionLambda, this.method.bodyStart, this.method.bodyStart));
					this.postconditionVariableDeclaration.initialization = preconditionLambdaCall;
				}	
			} else {
				if (!(this.method instanceof ConstructorDeclaration))
					this.statementsForMethodBody.add(preconditionLambdaCall);
			}
			blockDeclarationsCount += 2 * oldExpressions.size();
			this.block = new Block(blockDeclarationsCount);
			this.block.statements = new Statement[statementsForBlock.size()];
			this.block.sourceStart = this.preconditions != null ? this.preconditions[0].sourceStart : this.postconditions != null ? this.postconditions[0].sourceStart : this.method.bodyStart;
			this.block.sourceEnd = this.method.bodyEnd;
			statementsForBlock.toArray(this.block.statements);
			if (this.method instanceof ConstructorDeclaration) {
				this.statementsForMethodBody.add(this.block);
			} else {
				this.preconditionLambda.setBody(this.block);
				this.preconditionLambda.sourceStart = this.method.bodyStart;
				this.preconditionLambda.sourceEnd = this.method.bodyEnd;
			}

			for (Statement s : this.statementsForMethodBody)
				s.resolve(this.method.scope);
		}
		
		int thisElementModifiers = this.method.binding.modifiers;
		ReferenceBinding thisClassBinding = this.method.binding.declaringClass;
		
		if (this.preconditions != null)
			for (Expression e : this.preconditions)
				check(thisElementModifiers, thisClassBinding, this.method.scope, e);
		if (this.throwsConditions != null)
			for (Expression e : this.throwsConditions)
				check(thisElementModifiers, thisClassBinding, this.method.scope, e);
		if (this.mayThrowConditions != null)
			for (Expression e : this.mayThrowConditions)
				check(thisElementModifiers, thisClassBinding, this.method.scope, e);
		if (this.postconditions != null)
			for (Expression e : this.postconditions)
				check(thisElementModifiers, thisClassBinding, this.method.scope, e);
		
		if (this.inspectsExpressions != null)
			for (Expression e : this.inspectsExpressions)
				check(thisElementModifiers, thisClassBinding, this.method.scope, e);
		if (this.mutatesExpressions != null)
			for (Expression e : this.mutatesExpressions)
				check(thisElementModifiers, thisClassBinding, this.method.scope, e);
		if (this.mutatesPropertiesExpressions != null)
			for (Expression e : this.mutatesPropertiesExpressions)
				check(thisElementModifiers, thisClassBinding, this.method.scope, e);
		// TODO(fs4j): Enable once @creates clauses are typechecked
//		if (this.createsExpressions != null)
//			for (Expression e : this.createsExpressions)
//				check(thisElementModifiers, thisClassBinding, this.method.scope, e);
	}
	
	public static void check(int thisElementModifiers, ReferenceBinding thisClassBinding, BlockScope thisScope, Expression e) {
		ASTVisitor checker = new ASTVisitor() {
			
			private boolean isVisible(int modifiers, PackageBinding packageBinding) {
				if ((modifiers & ClassFileConstants.AccPublic) != 0)
					return true;
				if ((thisElementModifiers & ClassFileConstants.AccPublic) != 0 && thisClassBinding.isPublic())
					return false;
				if ((thisElementModifiers & ClassFileConstants.AccPrivate) != 0)
					return true;
				if ((modifiers & ClassFileConstants.AccPrivate) != 0)
					return false;
				// Here, both elements are either protected or package-accessible
				if (packageBinding != thisClassBinding.fPackage)
					return false;
				if ((thisElementModifiers & ClassFileConstants.AccProtected) != 0) {
					// TODO(fsc4j): More checks are required here
					if (!((modifiers & ClassFileConstants.AccProtected) != 0))
						return false;
				}
				return true;
			}
			
			private boolean isVisible(ReferenceBinding binding) {
				return binding == null || isVisible(binding.modifiers, binding.fPackage);
			}
			
			private boolean isVisible(TypeBinding binding) {
				if (binding instanceof ArrayBinding)
					return isVisible(((ArrayBinding)binding).leafComponentType);
				else if (binding instanceof ReferenceBinding)
					return isVisible((ReferenceBinding)binding);
				else
					return true;
			}

			private boolean isVisible(MethodBinding binding) {
				if (binding == null || binding.declaringClass == null)
					return true;
				if (!isVisible(binding.declaringClass))
					return false;
				return isVisible(binding.modifiers, binding.declaringClass.fPackage);
			}
			
			private void checkTypeReference(ASTNode node, TypeBinding binding) {
				if (binding != null)
					if (!isVisible(binding))
						thisScope.problemReporter().notVisibleType(node, binding);
			}
			
			private void checkConstructor(ASTNode node, MethodBinding binding) {
				if (binding != null)
					if (!isVisible(binding))
						thisScope.problemReporter().notVisibleConstructor(node, binding);
			}

			@Override
			public boolean visit(AllocationExpression allocationExpression, BlockScope scope) {
				checkConstructor(allocationExpression, allocationExpression.binding);					
				return true;
			}

			@Override
			public boolean visit(ArrayAllocationExpression arrayAllocationExpression, BlockScope scope) {
				checkTypeReference(arrayAllocationExpression, arrayAllocationExpression.resolvedType);
				return true;
			}

			@Override
			public boolean visit(ArrayQualifiedTypeReference arrayQualifiedTypeReference, BlockScope scope) {
				checkTypeReference(arrayQualifiedTypeReference, arrayQualifiedTypeReference.resolvedType);
				return true;
			}

			@Override
			public boolean visit(ArrayQualifiedTypeReference arrayQualifiedTypeReference, ClassScope scope) {
				checkTypeReference(arrayQualifiedTypeReference, arrayQualifiedTypeReference.resolvedType);
				return true;
			}

			@Override
			public boolean visit(ArrayTypeReference arrayTypeReference, BlockScope scope) {
				checkTypeReference(arrayTypeReference, arrayTypeReference.resolvedType);
				return true;
			}

			@Override
			public boolean visit(ArrayTypeReference arrayTypeReference, ClassScope scope) {
				checkTypeReference(arrayTypeReference, arrayTypeReference.resolvedType);
				return true;
			}
			
			private void checkAssignment(ASTNode node) {
				thisScope.problemReporter().assignmentInJavadoc(node);
			}

			@Override
			public boolean visit(Assignment assignment, BlockScope scope) {
				checkAssignment(assignment);
				return true;
			}

			@Override
			public boolean visit(ClassLiteralAccess classLiteral, BlockScope scope) {
				checkTypeReference(classLiteral, classLiteral.resolvedType);
				return super.visit(classLiteral, scope);
			}

			@Override
			public boolean visit(CompoundAssignment compoundAssignment, BlockScope scope) {
				checkAssignment(compoundAssignment);
				return super.visit(compoundAssignment, scope);
			}
			
			private void checkFieldReference(ASTNode node, FieldBinding binding) {
				if (binding != null && binding.declaringClass != null) // https://github.com/fsc4j/fsc4j/issues/4
					if (!isVisible(binding.declaringClass) || !isVisible(binding.modifiers, binding.declaringClass.fPackage))
						thisScope.problemReporter().notVisibleField(node, binding);
			}

			@Override
			public boolean visit(FieldReference fieldReference, BlockScope scope) {
				checkFieldReference(fieldReference, fieldReference.binding);
				return super.visit(fieldReference, scope);
			}

			@Override
			public boolean visit(FieldReference fieldReference, ClassScope scope) {
				checkFieldReference(fieldReference, fieldReference.binding);
				return super.visit(fieldReference, scope);
			}
			
			private void checkMethodReference(long nameSourcePosition, MethodBinding binding) {
				if (binding != null && binding.declaringClass != null && !(binding instanceof ProblemMethodBinding)) // https://github.com/fsc4j/fsc4j/issues/13
					if (!isVisible(binding.declaringClass) || !isVisible(binding.modifiers, binding.declaringClass.fPackage))
						thisScope.problemReporter().notVisibleMethod(nameSourcePosition, binding);
			}

			@Override
			public boolean visit(MessageSend messageSend, BlockScope scope) {
				checkMethodReference(messageSend.nameSourcePosition, messageSend.binding);
				return super.visit(messageSend, scope);
			}

			@Override
			public boolean visit(ParameterizedQualifiedTypeReference parameterizedQualifiedTypeReference,
					BlockScope scope) {
				checkTypeReference(parameterizedQualifiedTypeReference, parameterizedQualifiedTypeReference.resolvedType);
				return super.visit(parameterizedQualifiedTypeReference, scope);
			}

			@Override
			public boolean visit(ParameterizedQualifiedTypeReference parameterizedQualifiedTypeReference,
					ClassScope scope) {
				checkTypeReference(parameterizedQualifiedTypeReference, parameterizedQualifiedTypeReference.resolvedType);
				return super.visit(parameterizedQualifiedTypeReference, scope);
			}

			@Override
			public boolean visit(ParameterizedSingleTypeReference parameterizedSingleTypeReference,
					BlockScope scope) {
				checkTypeReference(parameterizedSingleTypeReference, parameterizedSingleTypeReference.resolvedType);
				return super.visit(parameterizedSingleTypeReference, scope);
			}

			@Override
			public boolean visit(ParameterizedSingleTypeReference parameterizedSingleTypeReference,
					ClassScope scope) {
				checkTypeReference(parameterizedSingleTypeReference, parameterizedSingleTypeReference.resolvedType);
				return super.visit(parameterizedSingleTypeReference, scope);
			}

			@Override
			public boolean visit(PostfixExpression postfixExpression, BlockScope scope) {
				checkAssignment(postfixExpression);
				return super.visit(postfixExpression, scope);
			}

			@Override
			public boolean visit(PrefixExpression prefixExpression, BlockScope scope) {
				checkAssignment(prefixExpression);
				return super.visit(prefixExpression, scope);
			}

			@Override
			public boolean visit(QualifiedAllocationExpression qualifiedAllocationExpression, BlockScope scope) {
				checkConstructor(qualifiedAllocationExpression, qualifiedAllocationExpression.binding);
				return super.visit(qualifiedAllocationExpression, scope);
			}
			
			private void checkBinding(ASTNode node, Binding binding) {
				if (binding instanceof TypeBinding)
					checkTypeReference(node, (TypeBinding)binding);
				if (binding instanceof FieldBinding)
					checkFieldReference(node, (FieldBinding)binding);
			}
			
			private void checkQualifiedNameReference(QualifiedNameReference reference) {
				checkBinding(reference, reference.binding);
				if (reference.otherBindings != null)
					for (int i = 0; i < reference.otherBindings.length; i++)
						checkBinding(reference, reference.otherBindings[i]);
			}

			@Override
			public boolean visit(QualifiedNameReference qualifiedNameReference, BlockScope scope) {
				checkQualifiedNameReference(qualifiedNameReference);
				return super.visit(qualifiedNameReference, scope);
			}

			@Override
			public boolean visit(QualifiedNameReference qualifiedNameReference, ClassScope scope) {
				checkQualifiedNameReference(qualifiedNameReference);
				return super.visit(qualifiedNameReference, scope);
			}

			@Override
			public boolean visit(QualifiedTypeReference qualifiedTypeReference, BlockScope scope) {
				checkTypeReference(qualifiedTypeReference, qualifiedTypeReference.resolvedType);
				return super.visit(qualifiedTypeReference, scope);
			}

			@Override
			public boolean visit(QualifiedTypeReference qualifiedTypeReference, ClassScope scope) {
				checkTypeReference(qualifiedTypeReference, qualifiedTypeReference.resolvedType);
				return super.visit(qualifiedTypeReference, scope);
			}

			@Override
			public boolean visit(SingleNameReference singleNameReference, BlockScope scope) {
				checkBinding(singleNameReference, singleNameReference.binding);
				return super.visit(singleNameReference, scope);
			}

			@Override
			public boolean visit(SingleNameReference singleNameReference, ClassScope scope) {
				checkBinding(singleNameReference, singleNameReference.binding);
				return super.visit(singleNameReference, scope);
			}

			@Override
			public boolean visit(SingleTypeReference singleTypeReference, BlockScope scope) {
				checkTypeReference(singleTypeReference, singleTypeReference.resolvedType);
				return super.visit(singleTypeReference, scope);
			}

			@Override
			public boolean visit(SingleTypeReference singleTypeReference, ClassScope scope) {
				checkTypeReference(singleTypeReference, singleTypeReference.resolvedType);
				return super.visit(singleTypeReference, scope);
			}

			@Override
			public boolean visit(ThrowStatement throwStatement, BlockScope scope) {
				scope.problemReporter().throwInJavadoc(throwStatement);
				return super.visit(throwStatement, scope);
			}

			@Override
			public boolean visit(TryStatement tryStatement, BlockScope scope) {
				scope.problemReporter().tryInJavadoc(tryStatement);
				return super.visit(tryStatement, scope);
			}
			
		};
		
		e.traverse(checker, thisScope);
	}

	public void generatePostconditionCheck(CodeStream codeStream) {
		if (this.postconditions != null || this.throwsConditions != null) {
			int returnType = this.method.binding.returnType.id;
			if (returnType == TypeIds.T_void) {
				codeStream.load(this.postconditionVariableDeclaration.binding);
				if (this.method instanceof ConstructorDeclaration)
					codeStream.aload_0();
			} else {
				if (returnType == TypeIds.T_long || returnType == TypeIds.T_double) {
					codeStream.dup2();
					codeStream.load(this.postconditionVariableDeclaration.binding);
					codeStream.dup_x2();
					codeStream.pop();
				} else {
					codeStream.dup();
					codeStream.load(this.postconditionVariableDeclaration.binding);
					codeStream.swap();
				}
				if (this.method.binding.returnType.isPrimitiveType())
					codeStream.generateBoxingConversion(returnType);
			}
			codeStream.aconst_null();
			MethodBinding generatedMethod = this.postconditionMethodCall.binding.original();
			TypeBinding constantPoolDeclaringClass = CodeStream.getConstantPoolDeclaringClass(this.method.scope, generatedMethod, generatedMethod.declaringClass, false);
			codeStream.invoke(Opcodes.OPC_invokeinterface, generatedMethod, constantPoolDeclaringClass);
		}
		
	}

	public FlowInfo analyseCode(MethodScope scope, ExceptionHandlingFlowContext methodContext, FlowInfo flowInfo) {
		for (Statement s : this.statementsForMethodBody)
			flowInfo = s.analyseCode(scope, methodContext, flowInfo);
		return flowInfo;
	}

	public void generateCode(MethodScope scope, CodeStream codeStream) {
		for (Statement s : this.statementsForMethodBody)
			s.generateCode(scope, codeStream);
	}

	public boolean hasEffectClauses() {
		return this.inspectsExpressions != null || this.mutatesExpressions != null || this.mutatesPropertiesExpressions != null || this.createsExpressions != null;
	}
	
	public int mutatesThisSourceLocation() {
		if (this.mutatesExpressions != null)
			for (Expression e : this.mutatesExpressions)
				if (e instanceof ThisReference)
					return e.sourceStart;
		if (this.mutatesPropertiesExpressions != null)
			for (Expression e : this.mutatesPropertiesExpressions)
				if (e instanceof MessageSend && ((MessageSend)e).receiver instanceof ThisReference)
					return e.sourceStart;
		return -1;
	}
	
	public int inspectsThisSourceLocation() {
		if (this.inspectsExpressions != null)
			for (Expression e : this.inspectsExpressions)
				if (e instanceof ThisReference)
					return e.sourceStart;
		return mutatesThisSourceLocation();
	}
	
	private TryStatement generateSevereLoggerMessageIfPossible(char[] msg, int sourceStart, int sourceEnd) {
		//blockDeclarationsCount += 2;
		
		
		//Method $getLogger = Class.forName("java.util.logging.logger").getMethod("getLogger", String.class);
		MessageSend loggerClass = new MessageSend();
		loggerClass.receiver = javaLangClassNameReference();
		loggerClass.selector = "forName".toCharArray(); //$NON-NLS-1$
		loggerClass.arguments = new Expression[] {new StringLiteral("java.util.logging.Logger".toCharArray(), sourceStart, sourceEnd, 0)}; //$NON-NLS-1$
		
		MessageSend getLoggerMethod = new MessageSend();
		getLoggerMethod.receiver = loggerClass;
		getLoggerMethod.selector = "getMethod".toCharArray(); //$NON-NLS-1$
		getLoggerMethod.arguments = new Expression[] {new StringLiteral("getLogger".toCharArray(), sourceStart, sourceEnd, 0), new ClassLiteralAccess(sourceEnd, javaLangString())}; //$NON-NLS-1$
		
		Assignment getLoggerMethodVariableAssigment = new Assignment(new SingleNameReference(GETLOGGER_METHOD_VARIABLE_NAME, (sourceStart << 32) | sourceEnd), getLoggerMethod, sourceEnd);
		
		
		//Method $severe =  Class.forName("java.util.logging.Logger").getMethod("severe", String.class); //$NON-NLS-1$ //$NON-NLS-2$
		MessageSend severeMethod = new MessageSend();
		severeMethod.receiver = loggerClass;
		severeMethod.selector = "getMethod".toCharArray(); //$NON-NLS-1$
		severeMethod.arguments = new Expression[] {new StringLiteral("severe".toCharArray(), sourceStart, sourceEnd, 0), new ClassLiteralAccess(sourceEnd, javaLangString())}; //$NON-NLS-1$
		
		Assignment severeMethodVariableAssigment = new Assignment(new SingleNameReference(SEVERE_METHOD_VARIABLE_NAME, (sourceStart << 32) | sourceEnd), severeMethod, sourceEnd);

		
		//$severe.invoke($getLogger.invoke(Class.forName("java.util.logging.Logger"), "fsc4j"), "hello world"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			//$getLogger.invoke(Class.forName("java.util.logging.Logger"), "fsc4j")
		MessageSend loggerClassArgument = new MessageSend();
		loggerClassArgument.receiver = javaLangClassNameReference();
		loggerClassArgument.selector = "forName".toCharArray(); //$NON-NLS-1$
		loggerClassArgument.arguments = new Expression[] {new StringLiteral("java.util.logging.Logger".toCharArray(), sourceStart, sourceEnd, 0)}; //$NON-NLS-1$
		
		MessageSend invokegetLoggerMethod = new MessageSend();
		invokegetLoggerMethod.receiver = new SingleNameReference(GETLOGGER_METHOD_VARIABLE_NAME, (this.method.bodyStart<< 32) + this.method.bodyStart);
		invokegetLoggerMethod.selector = "invoke".toCharArray(); //$NON-NLS-1$
		invokegetLoggerMethod.arguments = new Expression[] {loggerClassArgument, new StringLiteral("fsc4j".toCharArray(), sourceStart, sourceEnd, 0)}; //$NON-NLS-1$
		
		MessageSend invokesevereMethod = new MessageSend();
		invokesevereMethod.receiver = new SingleNameReference(SEVERE_METHOD_VARIABLE_NAME, (this.method.bodyStart<< 32) + this.method.bodyStart);
		invokesevereMethod.selector = "invoke".toCharArray(); //$NON-NLS-1$
		invokesevereMethod.arguments = new Expression[] {invokegetLoggerMethod, new StringLiteral(msg, sourceStart, sourceEnd, 0)};

		
		Block tryBlock = new Block(0);
		tryBlock.statements = new Statement[] {
				getLoggerMethodVariableAssigment,
				severeMethodVariableAssigment,
				invokesevereMethod
		};
		
		return generateTryCatchBlock(tryBlock, "$throw".toCharArray(), javaLangException(), null, sourceStart, sourceEnd); //$NON-NLS-1$
	}
	
	private TryStatement generateTryCatchBlock(Block tryBody, char[] catchExceptionArgumentName, QualifiedTypeReference catchExceptionType, Statement[] catchBlockStatements, int sourceStart, int sourceEnd) {
		TryStatement tryMethodStatement = new TryStatement();
		tryMethodStatement.tryBlock = tryBody;
		Argument catchExceptionArgument = new Argument(catchExceptionArgumentName, 0, catchExceptionType, 0);
		catchExceptionArgument.sourceStart = sourceStart;
		catchExceptionArgument.sourceEnd = sourceEnd;
		tryMethodStatement.catchArguments = new Argument[] {catchExceptionArgument};

		Block catchMethodExceptionBlock = new Block(0);
		catchMethodExceptionBlock.sourceStart = sourceStart;
		catchMethodExceptionBlock.sourceEnd = sourceEnd;
		catchMethodExceptionBlock.statements = catchBlockStatements;
		catchMethodExceptionBlock.scope = this.method.scope; 
		tryMethodStatement.catchBlocks = new Block[] {catchMethodExceptionBlock};
		tryMethodStatement.scope = this.method.scope;
		
		return tryMethodStatement;
	}
	
	private ASTVisitor generateOldExpressionASTVisitor(HashMap<String, OldExpression.DistinctExpression> oldExpressions, ArrayList<Statement> statementsForBlock) {
		return new ASTVisitor() {
			@Override
			public boolean visit(OldExpression oldExpression, BlockScope blockScope) {
				char[] name = Arrays.copyOf(oldExpression.source, oldExpression.source.length);
				for (int i = 0; i < name.length; i++) { // JVMS 4.2.2 field names cannot contain . ; [ / .
					switch (name[i]) {
						case '.': name[i] = '\u2024'; break; // ONE DOT LEADER
						case ';': name[i] = '\u204f'; break; // REVERSED SEMICOLON 
						case '[': name[i] = '\u298b'; break; // LEFT SQUARE BRACKET WITH UNDERBAR
						case ']': name[i] = '\u298c'; break; // RIGHT SQUARE BRACKET WITH UNDERBAR
						case '/': name[i] = '\u2afd'; break; // DOUBLE SOLIDUS OPERATOR
					}
				}
				char[] innerName = CharOperation.concat(name, OLD_VARIABLE_INNER_SUFFIX);
				char[] exceptionName = CharOperation.concat(name, OLD_VARIABLE_EXCEPTION_SUFFIX);
				String nameString = String.valueOf(name);
				OldExpression.DistinctExpression distinctExpression = oldExpressions.get(nameString);
				long pos = (oldExpression.sourceStart << 32) + oldExpression.sourceEnd;
				if (distinctExpression == null) {
					distinctExpression = new OldExpression.DistinctExpression();
					oldExpressions.put(nameString, distinctExpression);
					
					distinctExpression.exceptionDeclaration = new LocalDeclaration(exceptionName, oldExpression.sourceStart, oldExpression.sourceEnd);
					distinctExpression.exceptionDeclaration.type = javaLangThrowable();
					distinctExpression.exceptionDeclaration.initialization = new NullLiteral(oldExpression.sourceStart, oldExpression.sourceEnd);
					statementsForBlock.add(distinctExpression.exceptionDeclaration);
					distinctExpression.outerDeclaration = new LocalDeclaration(name, oldExpression.sourceStart, oldExpression.sourceEnd);
					distinctExpression.outerDeclaration.type = javaLangObject();
					distinctExpression.outerDeclaration.initialization = new NullLiteral(oldExpression.sourceStart, oldExpression.sourceEnd);
					statementsForBlock.add(distinctExpression.outerDeclaration);

					Block tryBlock = new Block(1);
					distinctExpression.innerDeclaration = new LocalDeclaration(innerName, oldExpression.sourceStart, oldExpression.sourceEnd);
					distinctExpression.innerDeclaration.type = new SingleTypeReference("var".toCharArray(), pos); //$NON-NLS-1$
					distinctExpression.innerDeclaration.initialization = oldExpression.expression;
					tryBlock.statements = new Statement[] {
							distinctExpression.innerDeclaration,
							new Assignment(new SingleNameReference(name, pos), new SingleNameReference(innerName, pos), oldExpression.sourceEnd)
					};
					
					char[] catchArgumentName = "$exception".toCharArray(); //$NON-NLS-1$
					Argument catchArgument = new Argument(catchArgumentName, pos, javaLangThrowable(), 0);
					Block catchBlock = new Block(0);
					catchBlock.statements = new Statement[] {
							new Assignment(new SingleNameReference(exceptionName, pos), new SingleNameReference(catchArgumentName, pos), oldExpression.sourceEnd)
					};
					
					TryStatement tryStatement = new TryStatement();
					tryStatement.tryBlock = tryBlock;
					tryStatement.catchArguments = new Argument[] {catchArgument};
					tryStatement.catchBlocks = new Block[] {catchBlock};
					statementsForBlock.add(tryStatement);
				}
				oldExpression.distinctExpression = distinctExpression;
				oldExpression.reference = new SingleNameReference(name, pos);
				oldExpression.exceptionReference = new SingleNameReference(exceptionName, pos);
				return false;
			}
			
		};

	}

}
