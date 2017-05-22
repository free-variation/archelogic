:- use_module(library(apply)).
:- use_module(library(yall)).


% Lambda Calculus
up(Term, [X]>>Formula) :-
	Term =.. [P | Args],
	append(Args, [X], ExtArgs),
	Formula =.. [P | ExtArgs].

down([_]>>Formula, Term) :-
	Formula =.. Atoms,
	append(TermAtoms, [_], Atoms), 
	Term =.. TermAtoms, !.

% the marked form of functional application 
% for handling existentially-closed arguments
f(Lambda, Arg, Reduced) :-
	Arg = {X}/Formula, !,
	lambda_calls(Lambda, [X], Applied),
	Reduced = {X}/(Formula, Applied).

f(Lambda, Arg, Reduced) :-
	lambda_calls(Lambda, [Arg], Reduced).

% Build Expression

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

predicate(rel(_, _, Word2), Relations, LogicalForm) :-
	Word2 = word(_, Predicate, _, _),

	% test if there's an object 
	ObjectRel = rel(dobj, Word2, _),
	(	member(ObjectRel, Relations) ->
		(	logical_form(ObjectRel, Relations, ObjectLF),
			up(Predicate, L1),
			f(L1, ObjectLF, PredicateLF)
		)
	;	PredicateLF = Predicate
	),

	% predicate the subject
	SubjectRel = rel(nsubj, Word2, _),
	member(SubjectRel, Relations),
	logical_form(SubjectRel, Relations, SubjectLF),
	up(PredicateLF, L),
	f(L, SubjectLF, LogicalForm).

