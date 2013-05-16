%% Author: jakob
%% Created: 24 nov 2009
%% Description: TODO: Add description to erlide_indent_tests
-module(erlide_indent_tests).

%%
%% Include files
%%

-include_lib("eunit/include/eunit.hrl").

%%
%% test Functions
%%

-define(Test_indent(SIndent, S),
        ?_assertEqual(SIndent,
                      erlide_indent:indent_lines(S, 0, length(S), 8, false, []))).

simple_function_test_() ->
    S = "a() ->\nb.\n",
    SIndent = "a() ->\n    b.\n",
    ?Test_indent(SIndent, S).

expressions_test_() ->
    S = "#r{a=a,\nb=b, [a,\nb],\n{a, b,\nc, fn(a, \nb)}},",
    SIndent = "#r{a=a,\n   b=b, [a,\n         b],\n   {a, b,\n    c, fn(a, \n          b)}},",
    ?Test_indent(SIndent, S).

try_catch_test_() ->
    S =
        ""++
            "cmd(Cmd, From, Args, Modules) ->\ntry\ncase get(logging) of\non ->\n"++
            "put(log, get(log)++[{Cmd, Args}]);\n_ ->\nok\nend,\n"++
            "case do_cmd(Cmd, Args, Modules) of\n{R, NewMods} ->\nreply(Cmd, From, R),\n"++
            "NewMods;\nNewMods ->\nreply(Cmd, From, ok),\nNewMods\nend\ncatch\n"++
            "exit:Error ->\nreply(Cmd, From, {exit, Error}),\nModules;\n"++
            "error:Error ->\nreply(Cmd, From, {error, Error}),\nModules\nend.",
    SIndent =
        ""++
            "cmd(Cmd, From, Args, Modules) ->\n"++
            "    try\n"++
            "        case get(logging) of\n"++
            "            on ->\n"++
            "                put(log, get(log)++[{Cmd, Args}]);\n"++
            "            _ ->\n"++
            "                ok\n"++
            "        end,\n"++
            "        case do_cmd(Cmd, Args, Modules) of\n"++
            "            {R, NewMods} ->\n"++
            "                reply(Cmd, From, R),\n"++
            "                NewMods;\n"++
            "            NewMods ->\n"++
            "                reply(Cmd, From, ok),\n"++
            "                NewMods\n"++
            "        end\n"++
            "    catch\n"++
            "        exit:Error ->\n"++
            "            reply(Cmd, From, {exit, Error}),\n"++
            "            Modules;\n"++
            "        error:Error ->\n"++
            "            reply(Cmd, From, {error, Error}),\n"++
            "            Modules\n"++
            "    end.",
    ?Test_indent(SIndent, S).

binary_1_test_() ->
    S =
        ""++
            "f() ->\n"++
            "<<1,\n"++
            "2>>.",
    SIndent =
        ""++
            "f() ->\n"++
            "    <<1,\n"++
            "      2>>.",
    ?Test_indent(SIndent, S).

%% http://www.assembla.com/spaces/erlide/tickets/595-indentation---doesn-t-handle-binaries-with-macros-or-expressions
binary_2_test_() ->
    S =
        ""++
            "g() ->\n"++
            "<<?M,\n"++
            "1>>.",
    SIndent =
        ""++
            "g() ->\n"++
            "    <<?M,\n"++
            "      1>>.",
    ?Test_indent(SIndent, S).

spec_test_() ->
    S = ""++
            "-spec start_link(config()) ->\n"++
            "{ok, pid()}.",
    SIndent = ""++
                  "-spec start_link(config()) ->\n"++
                  "          {ok, pid()}.",
    ?Test_indent(SIndent, S).

spec_2_test_() ->
    S = ""++
            "-spec start_link(config()) ->\n"++
            "{ok, pid()}.\n"++
            "f()->\n"++
            "ok.",
    SIndent = ""++
                  "-spec start_link(config()) ->\n"++
                  "          {ok, pid()}.\n"++
                  "f()->\n"++
                  "    ok.",
    ?Test_indent(SIndent, S).

spec_3_test_() ->
    S = ""++
            "-spec(start_link(config()) ->\n"++
            "{ok, pid()}).\n"++
            "f()->\n"++
            "ok.",
    SIndent = ""++
                  "-spec(start_link(config()) ->\n"++
                  "          {ok, pid()}).\n"++
                  "f()->\n"++
                  "    ok.",
    ?Test_indent(SIndent, S).

export_test_() ->
    S = ""++
            "-export([f/1,\n"++
            "f/2]).",
    SIndent = ""++
                  "-export([f/1,\n"++
                  "         f/2]).",
    ?Test_indent(SIndent, S).


%% binary comprehensions
%% http://www.assembla.com/spaces/erlide/tickets/729-indent--can-t-handle-binary-compehensions
binary_3_test_() ->
    S = ""++
            "foo(BS) ->\n"++
            "S = [A || <<A>> <= BS],\n"++
            "ok.",
    SIndent = ""++
                  "foo(BS) ->\n"++
                  "    S = [A || <<A>> <= BS],\n"++
                  "    ok.",
    ?Test_indent(SIndent, S).

%%
%% http://www.assembla.com/spaces/erlide/tickets/787-indent---confused-by-macros-in-case-clauses
macros_in_predicates_test_() ->
    S = ""++
            "foo() ->\n"++
            "case A of\n"++
            "?B when C == ?D ;\n"++
            "E == F ->",
    SIndent = ""++
                  "foo() ->\n"++
                  "    case A of\n"++
                  "        ?B when C == ?D ;\n"++
                  "                E == F ->",
    ?Test_indent(SIndent, S).

%%
type_test_() ->
    S = "" ++
            "-type mod_deps() :: dict().\n"++
            "a() ->\n"++
            "ok.\n",
    SIndent = "" ++
                  "-type mod_deps() :: dict().\n"++
                  "a() ->\n"++
                  "    ok.\n",
    ?Test_indent(SIndent, S).

%% http://www.assembla.com/spaces/erlide/tickets/936-indent--macros-in-list-comprehensions
macro_in_lc_test_() ->
    S = "" ++
            "b() ->\n"++
            "[?X(A) || X <-L],\n"++
            "a.\n",
    I = "" ++
            "b() ->\n"++
            "    [?X(A) || X <-L],\n"++
            "    a.\n",
    ?Test_indent(I, S).

%% http://www.assembla.com/spaces/erlide/tickets/776
%% indentation: receive..after is wrong
indent_after_test_() ->
    S = "" ++
            "a()->\n"++
            "receive\n"++
            "X ->\n"++
            "ok\n"++
            "after 500 ->\n"++
            "error\n"++
            "end.\n",
    I = "" ++
            "a()->\n"++
            "    receive\n"++
            "        X ->\n"
            "            ok\n"++
            "    after 500 ->\n"++
            "            error\n"++
            "    end.\n",
    ?Test_indent(I, S).

%% http://www.assembla.com/spaces/erlide/tickets/1083-indentation--bad-after--spec-with-when-clause
indent_spec_with_when_test_() ->
    S = "" ++
            "-spec a(T) -> ok when T::term().\n"++
            "a(apa) ->\n"++
            "ok.\n",
    I = "" ++
            "-spec a(T) -> ok when T::term().\n"++
            "a(apa) ->\n"++
            "    ok.\n",
    ?Test_indent(I, S).

%% http://assembla.com/spaces/erlide/tickets/1151-indent--fails-for-catch-with-guards
indent_catch_with_guards_test_() ->
    S = "" ++
            "f() ->\n"++
            "try\n"++
            "a\n"++
            "catch\n"++
            "A when is_tuple(A) ->\n"++
            "A\n"++
            "end.\n",
    I = "" ++
            "f() ->\n"++
            "    try\n"++
            "        a\n"++
            "    catch\n"++
            "        A when is_tuple(A) ->\n"++
            "            A\n"++
            "    end.\n",
    ?Test_indent(I, S).

indent_newline_char_test_() ->
    S = "" ++
            "a()->\n"++
            "foo(x, $\\n, y),\n"++
            "boo(),\n" ++
            "ok.\n",
    I = "" ++
            "a()->\n"++
            "    foo(x, $\\n, y),\n"++
            "    boo(),\n" ++
            "    ok.\n",
    ?Test_indent(I, S).

indent_emacs_record1_test_() ->
    S="
-record(record1, {a,
 b,
 c
}).
",
    I = "
-record(record1, {a,
                  b,
                  c
                 }).
",
    ?Test_indent(I, S).

indent_emacs_record2_test_() ->
    S="
-record(record2, {
  a,
  b
 }).
",
    I = "
-record(record2, {
          a,
          b
         }).
",
    ?Test_indent(I, S).

indent_emacs_record3_test_() ->
    S="
-record(record3, {a = 8#42423 bor
    8#4234,
  b = 8#5432
      bor 2#1010101
  c = 123 +
234,
      d}).
",
    I = "
-record(record3, {a = 8#42423 bor
                      8#4234,
                  b = 8#5432
                      bor 2#1010101
                  c = 123 +
                      234,
                  d}).
",
    ?Test_indent(I, S).

indent_emacs_record4_test_() ->
        S="
-record(record4, {
 a = 8#42423 bor
   8#4234,
 b = 8#5432
    bor 2#1010101
 c = 123 +
     234,
 d}).
",
I="
-record(record4, {
          a = 8#42423 bor
              8#4234,
          b = 8#5432
              bor 2#1010101
          c = 123 +
              234,
          d}).
",
    ?Test_indent(I, S).

indent_emacs_type01_test_() ->
    S = "
-type ann2() :: Var ::
'return'
| 'return_white_spaces'
| 'return_comments'
| 'text' | ann().
",
    I = "
-type ann2() :: Var ::
            'return'
          | 'return_white_spaces'
          | 'return_comments'
          | 'text' | ann().
",
    ?Test_indent(I, S).

indent_emacs_type02_test_() ->
    S = "
-type t15() :: {binary(),<<>>,<<_:34>>,<<_:_*42>>,
    <<_:3,_:_*14>>,<<>>} | [<<>>|<<_:34>>|<<_:16>>|
<<_:3,_:_*1472>>|<<_:19,_:_*14>>| <<_:34>>|
<<_:34>>|<<_:34>>|<<_:34>>].
-type t19() :: fun((t18()) -> t16()) |
  fun((nonempty_maybe_improper_list('integer', any())|
      1|2|3|a|b|<<_:3,_:_*14>>|integer()) ->
nonempty_maybe_improper_list('integer', any())|
1|2|3|a|b|<<_:3,_:_*14>>|integer()).
-type t25() :: #rec3{f123 :: [t24() |
1|2|3|4|a|b|c|d|
nonempty_maybe_improper_list(integer, any())]}.
",
    I = "
-type t15() :: {binary(),<<>>,<<_:34>>,<<_:_*42>>,
                <<_:3,_:_*14>>,<<>>} | [<<>>|<<_:34>>|<<_:16>>|
                                        <<_:3,_:_*1472>>|<<_:19,_:_*14>>| <<_:34>>|
                                        <<_:34>>|<<_:34>>|<<_:34>>].
-type t19() :: fun((t18()) -> t16()) |
               fun((nonempty_maybe_improper_list('integer', any())|
                    1|2|3|a|b|<<_:3,_:_*14>>|integer()) ->
                          nonempty_maybe_improper_list('integer', any())|
                          1|2|3|a|b|<<_:3,_:_*14>>|integer()).
-type t25() :: #rec3{f123 :: [t24() |
                              1|2|3|4|a|b|c|d|
                              nonempty_maybe_improper_list(integer, any())]}.
",
    ?Test_indent(I, S).

indent_emacs_spec03_test_() ->
    S = "
-type t99() ::
{t2(),t4(),t5(),t6(),t7(),t8(),t10(),t14(),
t15(),t20(),t21(), t22(),t25()}.
-spec t1(FooBar :: t99()) -> t99();
(t2()) -> t2();
  (t4()) -> t4() when is_subtype(t4(), t24);
(t23()) -> t23() when is_subtype(t23(), atom()),
                      is_subtype(t23(), t14());
(t24()) -> t24() when is_subtype(t24(), atom()),
                      is_subtype(t24(), t14()),
     is_subtype(t24(), t4()).

-spec over(I :: integer()) -> R1 :: foo:typen();
  (A :: atom()) -> R2 :: foo:atomen();
 (T :: tuple()) -> R3 :: bar:typen().

-spec mod:t2() -> any().

-spec handle_cast(Cast :: {'exchange', node(), [[name(),...]]}
  | {'del_member', name(), pid()},
     #state{}) -> {'noreply', #state{}}.

-spec handle_cast(Cast ::
   {'exchange', node(), [[name(),...]]}
 | {'del_member', name(), pid()},
   #state{}) -> {'noreply', #state{}}.
",
    I = "
-type t99() ::
        {t2(),t4(),t5(),t6(),t7(),t8(),t10(),t14(),
         t15(),t20(),t21(), t22(),t25()}.
-spec t1(FooBar :: t99()) -> t99();
        (t2()) -> t2();
        (t4()) -> t4() when is_subtype(t4(), t24);
        (t23()) -> t23() when is_subtype(t23(), atom()),
                              is_subtype(t23(), t14());
        (t24()) -> t24() when is_subtype(t24(), atom()),
                              is_subtype(t24(), t14()),
                              is_subtype(t24(), t4()).

-spec over(I :: integer()) -> R1 :: foo:typen();
          (A :: atom()) -> R2 :: foo:atomen();
          (T :: tuple()) -> R3 :: bar:typen().

-spec mod:t2() -> any().

-spec handle_cast(Cast :: {'exchange', node(), [[name(),...]]}
                        | {'del_member', name(), pid()},
                  #state{}) -> {'noreply', #state{}}.

-spec handle_cast(Cast ::
                    {'exchange', node(), [[name(),...]]}
                  | {'del_member', name(), pid()},
                  #state{}) -> {'noreply', #state{}}.
",
    ?Test_indent(I, S).

indent_emacs_spec04_test_() ->
    S = "
-spec all(fun((T) -> boolean()), List :: [T]) ->
 boolean() when is_subtype(T, term()). % (*)

-spec get_closest_pid(term()) ->
       Return :: pid()
 | {'error', {'no_process', term()}
   | {'no_such_group', term()}}.

",
    I = "
-spec all(fun((T) -> boolean()), List :: [T]) ->
                 boolean() when is_subtype(T, term()). % (*)

-spec get_closest_pid(term()) ->
                             Return :: pid()
                                     | {'error', {'no_process', term()}
                                        | {'no_such_group', term()}}.
",
    ?Test_indent(I, S).

indent_emacs_type_record_test_() ->
    S = "
-opaque attributes_data() ::
[{'column', column()} | {'line', info_line()} |
 {'text', string()}] |  {line(),column()}.
-record(r,{
    f1 :: attributes_data(),
f222 = foo:bar(34, #rec3{}, 234234234423,
               aassdsfsdfsdf, 2234242323) ::
[t24() | 1|2|3|4|a|b|c|d|
 nonempty_maybe_improper_list(integer, any())],
f333 :: [t24() | 1|2|3|4|a|b|c|d|
   nonempty_maybe_improper_list(integer, any())],
f3 = x:y(),
f4 = x:z() :: t99(),
f17 :: 'undefined',
f18 :: 1 | 2 | 'undefined',
f19 = 3 :: integer()|undefined,
f5 = 3 :: undefined|integer()}).

-record(state, {
  sequence_number = 1          :: integer()
  }).
",
    I = "
-opaque attributes_data() ::
          [{'column', column()} | {'line', info_line()} |
           {'text', string()}] |  {line(),column()}.
-record(r,{
          f1 :: attributes_data(),
          f222 = foo:bar(34, #rec3{}, 234234234423,
                         aassdsfsdfsdf, 2234242323) ::
                   [t24() | 1|2|3|4|a|b|c|d|
                    nonempty_maybe_improper_list(integer, any())],
          f333 :: [t24() | 1|2|3|4|a|b|c|d|
                   nonempty_maybe_improper_list(integer, any())],
          f3 = x:y(),
          f4 = x:z() :: t99(),
          f17 :: 'undefined',
          f18 :: 1 | 2 | 'undefined',
          f19 = 3 :: integer()|undefined,
          f5 = 3 :: undefined|integer()}).

-record(state, {
          sequence_number = 1          :: integer()
         }).
",
    ?Test_indent(I, S).

indent_emacs_comments_test_() ->
    S = "
%%%  Left

%%   Indented

%    Right
",
    I = "
%%%  Left

%%   Indented

                                                %    Right
",
    ?Test_indent(I, S).

indent_emacs_basics_test_() ->
    S = "
indent_basics(X, Y, Z)
  when X > 42,
Z < 13;
Y =:= 4711 ->
 %% comments
 % right comments
 case lists:filter(fun(_, AlongName,
        B,
    C) ->
           true
       end,
       [a,v,b])
 of
 [] ->
 Y = 5 * 43,
 ok;
 [_|_] ->
 Y = 5 * 43,
 ok
 end,
 Y,
 %% List, tuples and binaries
 [a,
  b, c
 ],
 [ a,
   b, c
  ],

 [
 a,
 b
],
 {a,
  b,c
 },
 { a,
  b,c
    },

 {
 a,
 b
 },

<<1:8,
  2:8
  >>,
 <<
 1:8,
 2:8
  >>,
 << 1:8,
   2:8
    >>,

 (a,
  b,
  c
 ),

    ( a,
     b,
     c
    ),


 (
  a,
  b,
  c
 ),

 call(2#42423 bor
   #4234,
  2#5432,
 other_arg),
 ok;
indent_basics(Xlongname,
     #struct{a=Foo,
     b=Bar},
       [X|
  Y]) ->
 testing_next_clause,
 ok;
indent_basics(          % AD added clause
 X,             % not sure how this should look
 Y,
 Z)
 when
 X < 42, Z > 13;
 Y =:= 4711 ->
 foo;
indent_basics(X, Y, Z) when      % AD added clause
 X < 42, Z > 13;        % testing when indentation
 Y =:= 4711 ->
 foo;
indent_basics(X, Y, Z)        % AD added clause
 when            % testing when indentation
 X < 42, Z > 13;        % unsure about this one
 Y =:= 4711 ->
 foo.
",
    I = "
indent_basics(X, Y, Z)
  when X > 42,
       Z < 13;
       Y =:= 4711 ->
    %% comments
                                                % right comments
    case lists:filter(fun(_, AlongName,
                          B,
                          C) ->
                              true
                      end,
                      [a,v,b])
    of
        [] ->
            Y = 5 * 43,
            ok;
        [_|_] ->
            Y = 5 * 43,
            ok
    end,
    Y,
    %% List, tuples and binaries
    [a,
     b, c
    ],
    [ a,
      b, c
    ],

    [
     a,
     b
    ],
    {a,
     b,c
    },
    { a,
      b,c
    },

    {
      a,
      b
    },

    <<1:8,
      2:8
    >>,
    <<
      1:8,
      2:8
    >>,
    << 1:8,
       2:8
    >>,

    (a,
     b,
     c
    ),

    ( a,
      b,
      c
    ),


    (
      a,
      b,
      c
    ),

    call(2#42423 bor
             #4234,
         2#5432,
         other_arg),
    ok;
indent_basics(Xlongname,
              #struct{a=Foo,
                      b=Bar},
              [X|
               Y]) ->
    testing_next_clause,
    ok;
indent_basics(                                        % AD added clause
  X,                                                 % not sure how this should look
  Y,
  Z)
  when
      X < 42, Z > 13;
      Y =:= 4711 ->
    foo;
indent_basics(X, Y, Z) when                        % AD added clause
      X < 42, Z > 13;                                % testing when indentation
      Y =:= 4711 ->
    foo;
indent_basics(X, Y, Z)                                % AD added clause
  when                                                % testing when indentation
      X < 42, Z > 13;                                % unsure about this one
      Y =:= 4711 ->
    foo.
",
    ?Test_indent(I, S).

indent_emacs_nested_test_() ->
    S = "
indent_nested() ->
 [
 {foo, 2, \"string\"},
 {bar, 3, \"another string\"}
  ].
",
    I = "
indent_nested() ->
    [
     {foo, 2, \"string\"},
     {bar, 3, \"another string\"}
    ].
",
    ?Test_indent(I, S).

indent_emacs_if_case_receive_test_() ->
    S = "
indent_icr(Z) ->         % icr = if case receive
 %% If
 if Z >= 0 ->
   X = 43 div 4,
   foo(X);
    Z =< 10 ->
   X = 43 div 4,
   foo(X);
    Z == 5 orelse
    Z == 7 ->
   X = 43 div 4,
   foo(X);
    true ->
   if_works
  end,
 %% Case
 case {Z, foo, bar} of
  {Z,_,_} ->
  X = 43 div 4,
  foo(X);
  {Z,_,_}  when
  Z =:= 42 ->        % AD line should be indented as a when
  X = 43 div 4,
  foo(X);
  {Z,_,_}
  when Z < 10 ->      % AD when should be indented
  X = 43 div 4,
  foo(X);
  {Z,_,_}
  when          % AD when should be indented
  Z < 10        % and the guards should follow when
  andalso        % unsure about how though
  true ->
  X = 43 div 4,
  foo(X)
 end,
 %% begin
 begin
 sune,
 X = 74234 + foo(8456) +
  345 div 43,
 ok
 end,


 %% receive
  receive
  {Z,_,_} ->
    X = 43 div 4,
    foo(X);
  Z ->
   X = 43 div 4,
   foo(X)
  end,
  receive
  {Z,_,_} ->
  X = 43 div 4,
  foo(X);
 Z           % AD added clause
  when Z =:= 1 ->      % This line should be indented by 2
  X = 43 div 4,
  foo(X);
 Z when          % AD added clause
  Z =:= 2 ->        % This line should be indented by 2
  X = 43 div 4,
  foo(X);
 Z ->
    X = 43 div 4,
    foo(X)
 after infinity ->
    foo(X),
    asd(X),
    5*43
 end,
 receive
 after 10 ->
 foo(X),
 asd(X),
 5*43
 end,
 ok.
",
    I = "
indent_icr(Z) ->                                 % icr = if case receive
    %% If
    if Z >= 0 ->
            X = 43 div 4,
            foo(X);
       Z =< 10 ->
            X = 43 div 4,
            foo(X);
       Z == 5 orelse
       Z == 7 ->
            X = 43 div 4,
            foo(X);
       true ->
            if_works
    end,
    %% Case
    case {Z, foo, bar} of
        {Z,_,_} ->
            X = 43 div 4,
            foo(X);
        {Z,_,_}        when
              Z =:= 42 ->                                % AD line should be indented as a when
            X = 43 div 4,
            foo(X);
        {Z,_,_}
          when Z < 10 ->                        % AD when should be indented
            X = 43 div 4,
            foo(X);
        {Z,_,_}
          when                                        % AD when should be indented
              Z < 10                                % and the guards should follow when
              andalso                                % unsure about how though
              true ->
            X = 43 div 4,
            foo(X)
    end,
    %% begin
    begin
        sune,
        X = 74234 + foo(8456) +
            345 div 43,
        ok
    end,


    %% receive
    receive
        {Z,_,_} ->
            X = 43 div 4,
            foo(X);
        Z ->
            X = 43 div 4,
            foo(X)
    end,
    receive
        {Z,_,_} ->
            X = 43 div 4,
            foo(X);
        Z                                         % AD added clause
          when Z =:= 1 ->                        % This line should be indented by 2
            X = 43 div 4,
            foo(X);
        Z when                                        % AD added clause
              Z =:= 2 ->                                % This line should be indented by 2
            X = 43 div 4,
            foo(X);
        Z ->
            X = 43 div 4,
            foo(X)
    after infinity ->
            foo(X),
            asd(X),
            5*43
    end,
    receive
    after 10 ->
            foo(X),
            asd(X),
            5*43
    end,
    ok.
",
    ?Test_indent(I, S).

indent_emacs_fun_test_() ->
    S = "
indent_fun() ->
    %% Changed fun to one indention level
Var = spawn(fun(X)
       when X == 2;
       X > 10 ->
  hello,
  case Hello() of
   true when is_atom(X) ->
      foo;
  false ->
      bar
  end;
 (Foo) when is_atom(Foo),
   is_integer(X) ->
  X = 6* 45,
  Y = true andalso
    kalle
 end),
  ok.
",
    I = "
indent_fun() ->
    %% Changed fun to one indention level
    Var = spawn(fun(X)
                      when X == 2;
                           X > 10 ->
                        hello,
                        case Hello() of
                            true when is_atom(X) ->
                                foo;
                            false ->
                                bar
                        end;
                   (Foo) when is_atom(Foo),
                              is_integer(X) ->
                        X = 6* 45,
                        Y = true andalso
                            kalle
                end),
    ok.
",
    ?Test_indent(I, S).

indent_emacs_try_catch_test_() ->
    S = "
indent_try_catch() ->
 try
 io:format(stdout, \"Parsing file ~s, \",
 [St0#leex.xfile]),
 {ok,Line3,REAs,Actions,St3} =
 parse_rules(Xfile, Line2, Macs, St2)
 catch
  exit:{badarg,R} ->
  foo(R),
  io:format(stdout,
   \"ERROR reason ~p~n\",
    R);
  error:R         % AD added clause
  when R =:= 42 ->      % when should be indented
  foo(R);
  error:R         % AD added clause
  when          % when should be indented
  R =:= 42 ->        % but unsure about this (maybe 2 more)
  foo(R);
  error:R when        % AD added clause
  R =:= foo ->        % line should be 2 indented (works)
  foo(R);
  error:R ->
  foo(R),
  io:format(stdout,
   \"ERROR reason ~p~n\",
    R)
 after
  foo('after'),
  file:close(Xfile)
 end;
indent_try_catch() ->
 try
   foo(bar)
 of
 X when true andalso
 kalle ->
  io:format(stdout, \"Parsing file ~s, \",
  [St0#leex.xfile]),
  {ok,Line3,REAs,Actions,St3} =
  parse_rules(Xfile, Line2, Macs, St2);
 X           % AD added clause
  when false andalso      % when should be 2 indented
  bengt ->
  gurka();
 X when          % AD added clause
  false andalso        % line should be 2 indented
  not bengt ->
  gurka();
 X ->
  io:format(stdout, \"Parsing file ~s, \",
  [St0#leex.xfile]),
  {ok,Line3,REAs,Actions,St3} =
  parse_rules(Xfile, Line2, Macs, St2)
 catch
 exit:{badarg,R} ->
    foo(R),
    io:format(stdout,
        \"ERROR reason ~p~n\",
        R);
 error:R ->
    foo(R),
    io:format(stdout,
        \"ERROR reason ~p~n\",
        R)
 after
 foo('after'),
  file:close(Xfile),
  bar(with_long_arg,
       with_second_arg)
 end;
 indent_try_catch() ->
 try foo()
 after
  foo(),
  bar(with_long_arg,
      with_second_arg)
 end.
",
    I = "
indent_try_catch() ->
    try
        io:format(stdout, \"Parsing file ~s, \",
                  [St0#leex.xfile]),
        {ok,Line3,REAs,Actions,St3} =
            parse_rules(Xfile, Line2, Macs, St2)
    catch
        exit:{badarg,R} ->
            foo(R),
            io:format(stdout,
                      \"ERROR reason ~p~n\",
                      R);
        error:R                                 % AD added clause
          when R =:= 42 ->                        % when should be indented
            foo(R);
        error:R                                 % AD added clause
          when                                        % when should be indented
              R =:= 42 ->                                % but unsure about this (maybe 2 more)
            foo(R);
        error:R when                                % AD added clause
              R =:= foo ->                                % line should be 2 indented (works)
            foo(R);
        error:R ->
            foo(R),
            io:format(stdout,
                      \"ERROR reason ~p~n\",
                      R)
    after
        foo('after'),
        file:close(Xfile)
    end;
indent_try_catch() ->
    try
        foo(bar)
    of
        X when true andalso
               kalle ->
            io:format(stdout, \"Parsing file ~s, \",
                      [St0#leex.xfile]),
            {ok,Line3,REAs,Actions,St3} =
                parse_rules(Xfile, Line2, Macs, St2);
        X                                         % AD added clause
          when false andalso                        % when should be 2 indented
               bengt ->
            gurka();
        X when                                        % AD added clause
              false andalso                                % line should be 2 indented
              not bengt ->
            gurka();
        X ->
            io:format(stdout, \"Parsing file ~s, \",
                      [St0#leex.xfile]),
            {ok,Line3,REAs,Actions,St3} =
                parse_rules(Xfile, Line2, Macs, St2)
    catch
        exit:{badarg,R} ->
            foo(R),
            io:format(stdout,
                      \"ERROR reason ~p~n\",
                      R);
        error:R ->
            foo(R),
            io:format(stdout,
                      \"ERROR reason ~p~n\",
                      R)
    after
        foo('after'),
        file:close(Xfile),
        bar(with_long_arg,
            with_second_arg)
    end;
indent_try_catch() ->
    try foo()
    after
        foo(),
        bar(with_long_arg,
            with_second_arg)
    end.
",
    ?Test_indent(I, S).

indent_emacs_catch_test_() ->
    S = "
indent_catch() ->
  D = B +
  float(43.1),

  B = catch oskar(X),

  A = catch (baz +
      bax),
 catch foo(),

  C = catch B +
     float(43.1),

  case catch foo(X) of
  A ->
 B
 end,

 case
  catch foo(X)
 of
 A ->
   B
  end,

  case
  foo(X)
   of
 A ->
    catch B,
    X
   end,

 try sune of
 _ -> foo
   catch _:_ -> baf
   end,

    try
sune
   of
 _ ->
   X = 5,
   (catch foo(X)),
   X + 10
   catch _:_ -> baf
   end,

   try
 (catch sune)
   of
 _ ->
   catch foo()  %% BUGBUG can't handle catch inside try without parentheses
   catch _:_ ->
    baf
   end,

   try
(catch exit())
   catch
_ ->
    catch baf()
   end,
 ok.
",
    I = "
indent_catch() ->
    D = B +
        float(43.1),

    B = catch oskar(X),

    A = catch (baz +
                   bax),
    catch foo(),

    C = catch B +
        float(43.1),

    case catch foo(X) of
        A ->
            B
    end,

    case
        catch foo(X)
    of
        A ->
            B
    end,

    case
        foo(X)
    of
        A ->
            catch B,
            X
    end,

    try sune of
        _ -> foo
    catch _:_ -> baf
    end,

    try
        sune
    of
        _ ->
            X = 5,
            (catch foo(X)),
            X + 10
    catch _:_ -> baf
    end,

    try
        (catch sune)
    of
        _ ->
            catch foo()  %% BUGBUG can't handle catch inside try without parentheses
    catch _:_ ->
            baf
    end,

    try
        (catch exit())
    catch
        _ ->
            catch baf()
    end,
    ok.
",
    ?Test_indent(I, S).

indent_emacs_binary_test_() ->
    S = "
indent_binary() ->
 X = lists:foldr(fun(M) ->
  <<Ma/binary, " ">>
 end, [], A),
 A = <<X/binary, 0:8>>,
B.
",
    I="
indent_binary() ->
    X = lists:foldr(fun(M) ->
                            <<Ma/binary, " ">>
                    end, [], A),
    A = <<X/binary, 0:8>>,
    B.
",
    ?Test_indent(I, S).

indent_emacs_comprehensions_test_() ->
    S = "
indent_comprehensions() ->
%% I don't have a good idea how we want to handle this
%% but they are here to show how they are indented today.
Result1 = [X ||
  #record{a=X} <- lists:seq(1, 10),
  true = (X rem 2)
     ],
Result2 = [X || <<X:32,_:32>> <= <<0:512>>,
    true = (X rem 2)
       ],

Binary1 = << <<X:8>> ||
  #record{a=X} <- lists:seq(1, 10),
  true = (X rem 2)
      >>,

Binary2 = << <<X:8>> || <<X:32,_:32>> <= <<0:512>>,
  true = (X rem 2)
      >>,
ok.
",
    I = "
indent_comprehensions() ->
    %% I don't have a good idea how we want to handle this
    %% but they are here to show how they are indented today.
    Result1 = [X ||
                  #record{a=X} <- lists:seq(1, 10),
                  true = (X rem 2)
              ],
    Result2 = [X || <<X:32,_:32>> <= <<0:512>>,
                    true = (X rem 2)
              ],

    Binary1 = << <<X:8>> ||
                  #record{a=X} <- lists:seq(1, 10),
                  true = (X rem 2)
              >>,

    Binary2 = << <<X:8>> || <<X:32,_:32>> <= <<0:512>>,
                            true = (X rem 2)
              >>,
    ok.
",
    ?Test_indent(I, S).

indent_emacs_regression_test_() ->
    S = "
%% This causes an error in earlier erlang-mode versions.
foo() ->
[#foo{
foo = foo}].
",
    I = "
%% This causes an error in earlier erlang-mode versions.
foo() ->
    [#foo{
        foo = foo}].
",
    ?Test_indent(I, S).

indent_emacs_long_names_01_test_() ->
    S = "
%% Record indentation
some_function_with_a_very_long_name() ->
 #'a-long-record-name-like-it-sometimes-is-with-asn.1-records'{
 field1=a,
 field2=b},
 case dummy_function_with_a_very_very_long_name(x) of
 #'a-long-record-name-like-it-sometimes-is-with-asn.1-records'{
 field1=a,
 field2=b} ->
 ok;
 Var = #'a-long-record-name-like-it-sometimes-is-with-asn.1-records'{
 field1=a,
 field2=b} ->
 Var#'a-long-record-name-like-it-sometimes-is-with-asn.1-records'{
 field1=a,
 field2=b};
 #xyz{
 a=1,
 b=2} ->
 ok
 end.
",
    I = "
%% Record indentation
some_function_with_a_very_long_name() ->
    #'a-long-record-name-like-it-sometimes-is-with-asn.1-records'{
       field1=a,
       field2=b},
    case dummy_function_with_a_very_very_long_name(x) of
        #'a-long-record-name-like-it-sometimes-is-with-asn.1-records'{
           field1=a,
           field2=b} ->
            ok;
        Var = #'a-long-record-name-like-it-sometimes-is-with-asn.1-records'{
                 field1=a,
                 field2=b} ->
            Var#'a-long-record-name-like-it-sometimes-is-with-asn.1-records'{
              field1=a,
              field2=b};
        #xyz{
           a=1,
           b=2} ->
            ok
    end.
",
    ?Test_indent(I, S).

indent_emacs_long_names_02_test_() ->
    S = "
another_function_with_a_very_very_long_name() ->
 #rec{
 field1=1,
 field2=1}.
",
    I = "
another_function_with_a_very_very_long_name() ->
    #rec{
       field1=1,
       field2=1}.
",
    ?Test_indent(I, S).

indent_emacs_long_names_03_test_() ->
    S = "
some_function_name_xyz(xyzzy, #some_record{
 field1=Field1,
 field2=Field2}) ->
 SomeVariable = f(#'Some-long-record-name'{
 field_a = 1,
 'inter-xyz-parameters' =
 #'Some-other-very-long-record-name'{
 field2 = Field1,
 field2 = Field2}}),
 {ok, SomeVariable}.
",
I="
some_function_name_xyz(xyzzy, #some_record{
                                 field1=Field1,
                                 field2=Field2}) ->
    SomeVariable = f(#'Some-long-record-name'{
                        field_a = 1,
                        'inter-xyz-parameters' =
                            #'Some-other-very-long-record-name'{
                               field2 = Field1,
                               field2 = Field2}}),
    {ok, SomeVariable}.
",
    ?Test_indent(I, S).


%%
%% Local Functions
%%

%% test_indent(SIndent, S) ->
%%     ?_assertEqual(SIndent, erlide_indent:indent_lines(S, 0, length(S), 8, false, [])).
