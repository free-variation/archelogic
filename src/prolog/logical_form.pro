:- use_module(library(apply)).
:- use_module(library(yall)).


join(I-J, J-K, I-K).

% the marked form of functional application 
% for handling existentially-closed arguments
f(Functor, Arg, Result) :-
	nonvar(Arg),
	Arg = {X}/Formula, !,
	f(Functor, X, Result1),
	join(Formula, [Result1 | F]-F, Formula1),
	Result = {X}/Formula1.

f(Functor, Arg, Result) :-
	Result =.. [Functor, Arg].

% two-place functors
% marked case where the second argument is existentially-closed.
f(Functor, Arg1, Arg2, Result) :-
	nonvar(Arg2),
	Arg2 = {X}/Formula, !,
	f(Functor, Arg1, X, Result1),
	join(Formula, [Result1 | F]-F, Formula1),
	Result = {X}/Formula1.

f(Functor, Arg1, Arg2, Result) :-
	Result =.. [Functor, Arg1, Arg2].

relations_of_type(Type, Relations, RelationsOfType) :-
	include({Type}/[Rel]>>(Rel=rel(Type, _, _)), Relations, RelationsOfType).

relations_for_governor(WordIndex, Relations, RelationsForGovernor) :-
	include({WordIndex}/[Rel]>>(Rel=rel(_, word(WordIndex, _, _, _), _)), Relations, RelationsForGovernor).

raise_existential_q({_}/(Terms-T), BareTerms) :-
	!,
	T = [],
	maplist(raise_existential_q, Terms, BareTerms).

raise_existential_q(Term, Term).

cnf(LogicalForm, cnf(Vars, FlatTerms)) :-
	raise_existential_q(LogicalForm, BareTerms),
	flatten(BareTerms, FlatTerms),
	term_variables(FlatTerms, Vars).
	

logical_form(Relations, CNF) :-
	is_list(Relations),
	relations_of_type(root, Relations, [RootRelation]),
	logical_form(RootRelation, Relations, LogicalForm),
	cnf(LogicalForm, CNF).

logical_form(RootRel, Relations, LogicalForm) :-
	RootRel = rel(root, _, _),
	predicate(RootRel, Relations, LogicalForm), !.
	
% default: return the dependent as an entity
logical_form(rel(_, _, word(_, Form, _, _)), _, Form).

% ----- Predicate Expressions -----

predicate(rel(_, _, Word2), Relations, LogicalForm) :-
	Word2 = word(WordIndex, Predicate, _, _),
	relations_for_governor(WordIndex, Relations, PredRelations),

	SubjectRel = rel(nsubj, Word2, _),
	member(SubjectRel, PredRelations),
	nominal(SubjectRel, Relations, SubjectLF),
	f(Predicate, E, Predicated),
	f(subject, E, SubjectLF, Subject),

	ObjectRel = rel(dobj, Word2, _),
	(	member(ObjectRel, PredRelations) ->
		(	nominal(ObjectRel, Relations, ObjectLF),
			f(object, E, ObjectLF, Object),
			LogicalForm = {E}/([Predicated, Subject, Object | T]-T)
		)
	;	LogicalForm = {E}/([Predicated, Subject | T]-T)
	).


% ----- Nominal Expressions ---
% leaf
nominal(rel(_, _, Word2), Relations, LogicalForm) :-
	Word2 = word(WordIndex, Head, _, _),
	relations_for_governor(WordIndex, Relations, []),
	LogicalForm = Head.

nominal(rel(_, _, Word2), Relations, LogicalForm) :-
	Word2 = word(WordIndex, Head, _, POS),
	relations_for_governor(WordIndex, Relations, HeadRels),
	(	POS = 'NNP' ->
		LF = (Head = X)
	;	LF =.. [Head, X]
	),
	dp(HeadRels, Relations, {X}/([LF | T]-T), LogicalForm).

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
			RelativeClauseLF = {E}/([Predicated, Subject, Object | T]-T)
		)
	;	RelativeClauseLF = {E}/([Predicated, Subject | T]-T)
	).
	

dp([], _, LogicalForm, LogicalForm).

dp([Rel | Rels], Relations, {X}/LF, LogicalForm) :-
 	Rel = rel('acl:relcl', _, _),
 	relative_clause(Rel, Relations, X, {_}/RelativeClauseLF),
 	join(LF, RelativeClauseLF, LF1),
 	dp(Rels, Relations, {X}/LF1, LogicalForm).









	



