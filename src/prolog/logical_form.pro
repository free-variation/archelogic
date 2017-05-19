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

f(Lambda, Arg, Reduced) :-
	lambda_calls(Lambda, [Arg], Reduced).

% Build Expression

relations_of_type(Type, Relations, RelationsOfType) :-
	include([Rel]>>(Rel=rel(Type, _, _)), Relations, RelationsOfType).

relations_for_governor(WordIndex, Relations, RelationsForGovernor) :-
	include([Rel]>>(Rel=rel(_, word(WordIndex, _, _, _), _)), Relations, RelationsForGovernor).

logical_form(Relations, LogicalForm) :-
	is_list(Relations),
	relations_of_type("root", Relations, RootRelation),
	logical_form(RootRelation, Relations, LogicalForm).

logical_form(rel("root", _, word(W, _, _, _)), Relations, LogicalForm) :-
	relations_for_governor(W, Relations, Rels),
	
