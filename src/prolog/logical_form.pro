:- use_module(library(apply)).
:- use_module(library(yall)).


debug(Value) :-
	nl, print('DEBUG '),
	print(Value),
	nl.

lambda_reduce(F, Goal) :-
	nonvar(F),
	F = beta(Lambda, Vars), 
	is_lambda(Lambda), !,
	lambda_calls(Lambda, Vars, ReducedLambda),
	lambda_reduce(ReducedLambda, Goal).

lambda_reduce(F, F) :- 
	nonvar(F),
	F = [], !.

lambda_reduce(F, F) :-
	var(F) ; atom(F).

lambda_reduce(F, Goal) :-
	nonvar(F), 
	\+atom(F),
	F =.. Terms,
	maplist(lambda_reduce, Terms, Terms1),
	Goal =.. Terms1.



% all humans run
%S ={X}/[P]>>(\+(human(X)); beta(P,[X])), P={}/[Y]>>E1^(runs(E1), subject(E1,Y)),lambda_reduce(beta(S, [P]),G).


% the marked form of functional application 
% for handling existentially-closed arguments
create_predicate(Functor, Arg, Result) :-
	nonvar(Arg),
	Arg = Vars^Formula, !,
	Vars = [X | _],
	f(Functor, X, Result1),
	Formula1 = (Formula, Result1),
	Result = Vars^Formula1.

create_predicate(Functor, Arg, Result) :-
	Result =.. [Functor, Arg].


relations_of_type(Type, Relations, RelationsOfType) :-
	include({Type}/[Rel]>>(Rel=rel(Type, _, _)), Relations, RelationsOfType).

relations_for_governor(WordIndex, Relations, RelationsForGovernor) :-
	include({WordIndex}/[Rel]>>(Rel=rel(_, word(WordIndex, _, _, _), _)), Relations, RelationsForGovernor).

member_object(X, List) :-
	List = [L | Rest],
	(	X == L
	;	member_object(X, Rest)
	).

raise_universal_q(X^(Terms-_), Vars) :-
	maplist(raise_universal_q, Terms, V),!,
	flatten(V, V1),
	exclude({X}/[Y]>>member_object(Y,X), V1, V2),
	list_to_set(V2, Vars).

raise_universal_q(Term, Vars) :-
	term_variables(Term, Vars).


skolemize(X, Vars, _) :-
	exclude({X}/[Y]>>(X==Y), Vars, Vars1),
	gensym(s, Functor),
	X =.. [Functor | Vars1].

raise_existential_q(Vars, X^(Terms-T), BareTerms) :-
	!,
	T = [],
	% skolemize(X, Vars, Terms),
	maplist(raise_existential_q(Vars), Terms, BareTerms).

raise_existential_q(_, Term, Term).

cnf(LogicalForm, cnf(Vars, FlatTerms)) :-
	term_variables(LogicalForm, Vars),
	raise_existential_q(Vars, LogicalForm, BareTerms),
	flatten(BareTerms, FlatTerms).
	

logical_form(Relations, LogicalForm) :-	
	is_list(Relations),
	relations_of_type(root, Relations, [RootRelation]),
	logical_form(RootRelation, Relations, LogicalForm).

logical_form(RootRel, Relations, LogicalForm) :-
	RootRel = rel(root, _, _),
	root_predicate(RootRel, Relations, LogicalForm), !.
	
% default: return the dependent as an entity
logical_form(rel(_, _, word(_, Form, _, _)), _, Form).

% ----- Predicate Expressions -----

root_predicate(Rel, Relations, LogicalForm) :-
	Rel = rel(_, _, Word2),
	Word2 = word(WordIndex, Predicate, _, _),
	relations_for_governor(WordIndex, Relations, PredRelations),

	SubjectRel = rel(nsubj, Word2, _),
	member(SubjectRel, PredRelations),
	nominal(SubjectRel, Relations, SubjectLF),

	predicate(Rel, Relations, PredicateLF),
	LogicalForm = beta(SubjectLF, [PredicateLF]).


% ----- Nominal Expressions ---

nominal(rel(_, _, Word2), Relations, LogicalForm) :-
	Word2 = word(WordIndex, Head, _, POS),
	relations_for_governor(WordIndex, Relations, HeadRels),
	(	POS = 'NNP' ->
		LF = (Head = X)
	;	LF =.. [Head, X]
	),
	(	HeadRels = [] ->
		LogicalForm = {}/[P]>>(X^(LF, beta(P, [X]))) 
	;	dp(HeadRels, Relations, X^LF, LogicalForm)
	).

is_pronominal(word(_, _, _, 'WP')).
is_pronominal(word(_, _, _, 'IN')).  % very unfortunate side-effect of dep parser.


relative_clause(rel('acl:relcl', _, Word2), Relations, X, RelativeClauseLF) :-
	Word2 = word(WordIndex, Predicate, _, _),
	relations_for_governor(WordIndex, Relations, PredRelations),

	SubjectRel = rel(nsubj, Word2, SubjectWord),
	member(SubjectRel, PredRelations),
	(	is_pronominal(SubjectWord ) ->
		SubjectLF = X
	;	nominal(SubjectRel, Relations, SubjectLF)
	),
	f(Predicate, E, Predicated),
	f(subject, E, SubjectLF, Subject),
 
	ObjectRel = rel(dobj, Word2, ObjectWord),
	% if so, predicate it
	(	member(ObjectRel, PredRelations) ->
		(	
			(	is_pronominal(ObjectWord) ->
				ObjectLF = X
			;	nominal(ObjectRel, Relations, ObjectLF)
			),
			f(object, E, ObjectLF, Object),
			RelativeClauseLF = [E]^(Predicated, Subject, Object)
		)
	;	RelativeClauseLF = [E]^(Predicated, Subject)
	).
	

dp([], _, LogicalForm, LogicalForm).

% relative clause
dp([Rel | Rels], Relations, X^LF, LogicalForm) :-
 	Rel = rel('acl:relcl', _, _),
 	X = [Y | _],
 	relative_clause(Rel, Relations, Y, E^RelativeClauseLF),
 	join(LF, RelativeClauseLF, LF1),
 	append(X, E, Vars),
 	dp(Rels, Relations, Vars^LF1, LogicalForm).


% determiner: all
dp([Rel | Rels], Relations, X^LF, LogicalForm) :-
	Rel = rel('det', _, word(_, _, all, 'DT')),
	dp(Rels, Relations, X^LF, LogicalForm).


% determiner: default (do nothing)
dp([Rel | Rels], Relations, X^LF, LogicalForm) :-
	Rel = rel('det', _, _),
	dp(Rels, Relations, X^LF, LogicalForm).



% ----- Predicate Expressions ---

predicate(rel(_, _, Word2), Relations, LogicalForm) :-
	Word2 = word(_, Head, _, _),
	Predicate =.. [Head, E],
	LogicalForm = {}/[X]>>E^(Predicate, subject(E, X)).





