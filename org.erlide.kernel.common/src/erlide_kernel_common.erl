-module(erlide_kernel_common).

%% Hack huaqiao, rewrite monitor node.

-export([
		 init/4,
		 halt_node/0
		]).

init(JRex, _Kill, HeapWarnLimit, HeapKillLimit) ->
	spawn(fun() ->
			   startup(JRex, HeapWarnLimit, HeapKillLimit)
		  end).

startup(JRex, HeapWarnLimit, HeapKillLimit)->
	erlide_jrpc:init(JRex),
	watch_eclipse(node(JRex)),
	
	erlide_monitor:start(HeapWarnLimit, HeapKillLimit),
	erlang:system_monitor(erlang:whereis(erlide_monitor),
						  [{long_gc, 3000}, {large_heap, HeapWarnLimit*1000000 div 2}]),
	
	%% disabled erl builder
	%% erlide_batch:start(erlide_builder),
	ok.

watch_eclipse(JavaNode) ->
	spawn(fun() ->
			   monitor_node(JavaNode, true),
			   erlide_log:log({"Monitoring java node", JavaNode}),
			   write_message({"start monitoring", JavaNode}),
			   receive
				   {nodedown, JavaNode}=_Msg ->
					   write_message(_Msg),
					   try_reconnect(JavaNode),
					   ok
			   end
		  end).

-define(MAX_RETRIES, 5).

try_reconnect(JavaNode) ->
	try_connect(JavaNode, 0).

try_connect(JavaNode, ?MAX_RETRIES) ->
	write_message({"Reconnect to JavaNode failed.", JavaNode}),
	halt_node();
try_connect(JavaNode, N) ->
	case net_adm:ping(JavaNode) of
		pong ->
			write_message({"Reconnected to JavaNode", JavaNode}),
			watch_eclipse(JavaNode);
		pang ->
			timer:sleep(500),
			write_message({"Reconnect to JavaNode, retry count: ("++integer_to_list(N)++")",
						   node()}),
			try_connect(JavaNode, N+1)
	end.

halt_node() ->
	erlang:halt().

%% shutdown() ->
%% 	erlide_monitor:stop(),
%% 	L = [V  || V = "erlide_" ++ _  <- [atom_to_list(X) || X <- registered()]],
%% 	[exit(whereis(list_to_atom(X)), kill) || X <- L],
%% 	ok.

write_message(Msg) ->
	{ok, [[Home]]} = init:get_argument(home),
	ok = filelib:ensure_dir(Home++"/.erlide/"),
	{ok, Fd} = file:open(Home++"/.erlide/erlide_debug.txt", [append, raw]),
	file:write(Fd, [lists:flatten(io_lib:format("~p: ~p got ~p~n",
											   [erlang:universaltime(), node(), Msg])), $\n]),
	file:sync(Fd),
	file:close(Fd),
	ok.
