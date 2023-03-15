package uk.ac.bris.cs.scotlandyard.model;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.*;
import uk.ac.bris.cs.scotlandyard.model.Piece.*;

import java.util.*;

import javax.annotation.Nonnull;

/**
 *   MyGameStateFactory is an implementation of Factory.
 */
public final class MyGameStateFactory implements Factory<GameState> {
	/**
	 * @param setup the game setup
	 * @param mrX MrX player
	 * @param detectives detective players
	 * @return GameState
	 */
	@Nonnull @Override public GameState build(	GameSetup setup,
												  Player mrX,
												  ImmutableList<Player> detectives) {
		return new MyGameState(setup,ImmutableSet.of(MrX.MRX),ImmutableSet.of(),mrX,detectives);
	}

	/**
	 *  MyGameState implements GameState instances.
	 */
	private final class MyGameState implements GameState {
		//According to cw-model manal list attributes..
		final private GameSetup setup;
		private final Player mrX;
		private final List<Player> detectives;
		private ImmutableList<LogEntry> log;
		private ImmutableList<LogEntry> mrXTravelLog;		//mrX travel log
		private ImmutableSet<Move> moves;
		private final ImmutableSet<Move> availableMoves ;	//all available moves
		private ImmutableSet<Piece> winner;					//winner of the game
		private final ImmutableSet<Piece> remaining = null;

		private final ImmutableMap<Detective, Integer> detectiveLocations = null;
		private final ImmutableMap<Piece, ImmutableMap<Ticket, Integer>> tickets = null;

		public MyGameState (GameSetup setup,
							Player mrX,
							ImmutableList<Player> detectives) {
			/* check for AllTest-GameStateCreationTest */
			if (detectives.isEmpty()) throw new IllegalArgumentException("Null Detective!");
			if (setup.moves.isEmpty()) throw new IllegalArgumentException("Move is empty!");  //copy form manual.


			this.mrX = mrX;
			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.detectives = detectives;
			//check for AllTest-GameStateCreationTest
			availableMoves = null;
		}

		@Nonnull @Override public GameSetup getSetup() { return setup; }
		@Nonnull @Override public ImmutableSet<Piece> getPlayers() { return tickets.keySet(); }
		@Nonnull @Override public Optional<Integer> getDetectiveLocation(Detective detective) {
			return Optional.ofNullable(detectiveLocations.get(detective));
		}
		@Nonnull @Override public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			return Optional.ofNullable(tickets.get(piece))
					.map(tickets -> ticket -> tickets.getOrDefault(ticket, 0));
		}
		@Nonnull @Override public ImmutableList<LogEntry> getMrXTravelLog() { return mrXTravelLog; }
		// copy form manual cw-model
		private static Set<Move.SingleMove> makeSingleMoves(GameSetup setup,
															List<Player> detectives, Player player, int source) {
			// TODO create an empty collection of some sort, say, HashSet, to store all the SingleMove we generate
			/*1.Created an empty set to store the generated moves.
				2.Added a flag to check if the destination is occupied by a detective, and if so, skip adding moves to that destination.
				3.Checked if the player has the required tickets for each transport, and only added a move if the player has the tickets.
				4.Added code to handle secret moves for MrX. If the current round is a secret round, and MrX has a secret ticket, then a move using the secret ticket is added.*/
			Set<Move.SingleMove> moves = new HashSet<>();
			boolean occupied = false;
			Move.SingleMove move;
			for (int destination : setup.graph.adjacentNodes(source)) {
				for (Player detective : detectives) {
					if (destination == detective.location()) {
						occupied = true;
						break;
					}
				}
				if (occupied) continue;


				// TODO find out if destination is occupied by a detective
				//  if the location is occupied, don't add to the collection of moves to return
				for (Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
					// TODO find out if the player has the required tickets
					if (player.has(t.requiredTicket())) {
						//  if it does, construct a SingleMove and add it the collection of moves to return

						move = new Move.SingleMove(player.piece(), source, t.requiredTicket(), destination);
						moves.add(move);
					}
				}
				// TODO consider the rules of secret moves here//black tickets

				//  add moves to the destination via a secret ticket if there are any left with the player


				// TODO return the collection of moves
				//if (player.isMrX() && setup..get(setup..size() - 1) == Round.SECRET) {
				if (player.isMrX() && player.hasAtLeast(Ticket.SECRET, 1)) {
					moves.add(new Move.SingleMove(player.piece(), source, Ticket.SECRET, destination));

				}
				return moves;

			}


//			private static Set<Move.DoubleMove> makeSingleMoves(GameSetup  setup, List<Player> detectives, Player player, int source){
//
//				// TODO create an empty collection of some sort, say, HashSet, to store all the SingleMove we generate
//				Set<Move.DoubleMove> moves = new HashSet<>();
//				for(int destination : setup.graph.adjacentNodes(source)) {
//					// TODO find out if destination is occupied by a detective
//					//  if the location is occupied, don't add to the collection of moves to return
//
//					for(Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of()) ) {
//						// TODO find out if the player has the required tickets
//						//  if it does, construct a SingleMove and add it the collection of moves to return
//					}
//
//					// TODO consider the rules of secret moves here
//					//  add moves to the destination via a secret ticket if there are any left with the player
//				}
//
//				// TODO return the collection of moves
//			}


			public GameState advance(Move move){
				MyGameState newState = new MyGameState ( setup, mrX, detectives);


			}
			@Nonnull @Override public ImmutableSet<Piece> getWinner(){
				return winner;
			}
			@Nonnull @Override public ImmutableSet<Move> getAvailableMoves(){
				return availableMoves;
			}

		}
}
}