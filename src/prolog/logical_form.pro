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

logical_form(Relations, LogicalForm) :-
	is_list(Relations),
	relations_of_type(root, Relations, [RootRelation]),
	logical_form(RootRelation, Relations, LogicalForm).

logical_form(RootRel, Relations, LogicalForm) :-
	RootRel = rel(root, _, _),
	predicate(RootRel, Relations, LogicalForm), !.
	
% default: return the dependent as an entity
logical_form(rel(_, _, word(_, Form, _, _)), _, Form).

% ----- Predicate Expressions -----

predicate(rel(_, _, Word2), Relations, LogicalForm) :-
	Word2 = word(WordIndex, Predicate, _, _),
	relations_for_governor(WordIndex, Relations, PredRelations),

	% predicate the subject
	SubjectRel = rel(nsubj, Word2, _),
	member(SubjectRel, PredRelations),
	nominal(SubjectRel, Relations, SubjectLF),
	f(Predicate, X, Predicated),
	f(subject, X, SubjectLF, Subject),

	% test if there's an object 
	ObjectRel = rel(dobj, Word2, _),
	% if so, predicate it
	(	member(ObjectRel, PredRelations) ->
		(	nominal(ObjectRel, Relations, ObjectLF),
			f(object, X, ObjectLF, Object),
			LogicalForm = {X}/([Predicated, Subject, Object | F]-F)
		)
	;	LogicalForm = {X}/([Predicated, Subject | F]-F)
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
	dp(HeadRels, {X}/([LF | F]-F), LogicalForm).


dp([], LogicalForm, LogicalForm).

dp([Rel | Rels], LF, LogicalForm) :-
	LogicalForm = LF.




	



