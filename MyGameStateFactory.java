package uk.ac.bris.cs.scotlandyard.model;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.*;
import uk.ac.bris.cs.scotlandyard.model.Piece.*;

import java.util.*;

import javax.annotation.Nonnull;
import javax.crypto.spec.PSource;

import static com.google.common.collect.ImmutableList.*;
//import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket;

/**
 *   MyGameStateFactory is an implementation of Factory.
 */
public final class MyGameStateFactory implements Factory<GameState> {
	/**
	 * @param setup      the game setup
	 * @param mrX        MrX player
	 * @param detectives detective players
	 * @return GameState
	 */
	@Nonnull
	@Override
	public GameState build(GameSetup setup,
						   Player mrX,
						   ImmutableList<Player> detectives) {
		//copy from manual cw-model doc
		return new MyGameState(setup, ImmutableSet.<MrX>of(MrX.MRX),ImmutableList.of(),mrX,detectives);
	}

	/**
	 * /**
	 * MyGameState implements GameState instances.
	 */
	private final class MyGameState implements GameState {
		//According to cw-model manal list attributes..
		final private GameSetup setup;
		private final Player mrX;
		private final List<Player> detectives;
		private ImmutableList<LogEntry> log;
		private ImmutableList<LogEntry> mrXTravelLog;        //mrX travel log
		private ImmutableSet<Move> moves;
		//private final ImmutableSet<Move> availableMoves;    //all available moves
		private ImmutableSet<Piece> winner;                    //winner of the game
		private ImmutableSet<Piece> remaining;

		private final ImmutableMap<Detective, Integer> detectiveLocations = null;
		//private final ImmutableMap<Piece, ImmutableMap<Ticket, Integer>> tickets = null;

		public MyGameState(GameSetup setup,
						   Player mrX,
						   ImmutableSet<Piece> remaining,
						   ImmutableList<LogEntry> log,
						   ImmutableList<Player> detectives) {
			/* check for AllTest-GameStateCreationTest */

			if (detectives.isEmpty())
				throw new IllegalArgumentException("Null Detective!");                        /* check for AllTest-GameStateCreationTest */
			if (setup.moves.isEmpty())
				throw new IllegalArgumentException("Move is empty!");                        //copy form manual.检查setup初始moves,graph是否存在
			if (setup.graph.nodes().isEmpty()) throw new IllegalArgumentException("Empty Graph!");
			if (!mrX.isMrX()) throw new IllegalArgumentException("Mrx is no black");

			for (Player d : detectives) {                                                                                //检查 1.detectives是否确实是detectives,位置是否重复，颜色是否有重复.
				if (!d.isDetective())
					throw new IllegalArgumentException("Detective is not detective");                // pass 3 test
				if (d.has(Ticket.DOUBLE)) throw new IllegalArgumentException("Detective have double ticket!");
				if (d.has(Ticket.SECRET)) throw new IllegalArgumentException("Detective have secret ticket!");

				for (Player otherD : detectives) {                                                                    // pass 2 test
					if ((d != otherD) && (d.piece().equals(otherD.piece())))
						throw new IllegalArgumentException("Same detective piece color!");
					if ((d != otherD) && (d.location() == otherD.location()))
						throw new IllegalArgumentException("Smae detecitve locations");
				}
			}

			this.mrX = mrX;
			this.setup = setup;
			this.remaining = remaining;
			this.mrXTravelLog = mrXTravelLog;
			this.detectives = detectives;
			this.moves = getAvailableMoves();
			this.winner = getWinner();  //check for AllTest-GameStateCreationTest
			//availableMoves = null;
		}

		@Nonnull
		@Override
		public GameSetup getSetup() {
			return setup;
		}

		@Nonnull
		@Override
		public ImmutableList<LogEntry> getMrXTravelLog() {
			return mrXTravelLog;
		}

		@Nonnull
		@Override
		public ImmutableSet<Piece> getPlayers() {
			Set<Piece> players = new HashSet<>();                                                                    //new treeset
			players.add(MrX.MRX);
			detectives.forEach(d -> players.add(d.piece()));
			return ImmutableSet.copyOf(players);
		}

		@Nonnull
		@Override
		public Optional<Integer> getDetectiveLocation(Detective detective) {
			for (Player player : detectives) {
				//compare this Piece object with the Detective object passed in as a parameter.
				if (player.piece() == detective) {
					// retrieve the current location of the detective and return it in an Optional
					return Optional.of((player.location()));
				}
			}

			return Optional.empty();
		}

		private Player getPlayer(Piece piece) {
			if (piece.isMrX()) return mrX;
			for (Player player : detectives) {
				if (player.piece() == piece) return player;
			}
			return null;
		}

		@Nonnull
		@Override
		public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			Player player = getPlayer(piece);
			if (player != null) {
				TicketBoard ticketBoard = new TicketBoard() {
					@Override
					public int getCount(@NotNull ScotlandYard.Ticket ticket) {

						return player.tickets().get(ticket);
					}
				};
				return Optional.of(ticketBoard);
			}
			return Optional.empty();
		}

		@Nonnull
		@Override
		public ImmutableSet<Move> getAvailableMoves() {
			Set<Move> availableMoves = new HashSet<>();
			//analyse the remaining players who can move
			for (Piece remainingPiece : remaining) {
				//convert it as a player object in accordance with piece
				Player remainPlayer = getPlayer(remainingPiece);
				ImmutableSet<Move.SingleMove> oneMoves = (ImmutableSet<Move.SingleMove>) makeSingleMoves(setup, detectives, remainPlayer, remainPlayer.location());
				//The number of the round the game needed to be run based on the size of mrXTravellog
				if (mrXTravelLog.size() + 1 <= setup.moves.size()) availableMoves.addAll(oneMoves);
				//only mrX can go two times when he was not at the last two round
				if ((remainingPiece.isMrX()) && (mrXTravelLog.size() + 2 <= setup.moves.size()) && (mrX.hasAtLeast(Ticket.DOUBLE, 1))) {
					availableMoves.addAll(makeDoubleMoves(setup, detectives, mrX, mrX.location()));
				}
			}
			return (ImmutableSet.copyOf(availableMoves));
		}

		private ImmutableSet<Move> getOneAvailableMoves(Piece piece) {
			Set<Move> availableMoves = new HashSet<Move>();
			Player player = Objects.requireNonNull(getPlayer(piece));                                                //根据piece,转换成Player对象
			ImmutableSet<Move.SingleMove> oneMoves = (ImmutableSet<Move.SingleMove>) makeSingleMoves(setup, detectives, player, player.location());        //Player的一步所有可能路径
			if (mrXTravelLog.size() + 1 <= setup.moves.size())                                                            //这里根据mrxtravellog的大小判断已经走到多少轮了
				availableMoves.addAll(oneMoves);                                                                    //如果还没走完，将其加入到可用列表中
			if ((piece.isMrX()) && (mrXTravelLog.size() + 2 <= setup.moves.size()) &&                                    //只有mrx有机会走两步，但是必须在至少还剩两轮的情况下使用
					mrX.hasAtLeast(Ticket.DOUBLE, 1)) {                                                        //可走步数+2要小于全部步数并且还有double卡
				availableMoves.addAll(makeDoubleMoves(setup, detectives, mrX, mrX.location()));
			}
			return (ImmutableSet.copyOf(availableMoves));                                                            //得到结果返回。具体single和double的实现见下面。
		}

		private static Set<Move.SingleMove> makeSingleMoves(GameSetup setup,
															List<Player> detectives, Player player, int source) {
			// TODO create an empty collection of some sort, say, HashSet, to store all the SingleMove we generate
			/*1.Created an empty set to store the generated moves.
				2.Added a flag to check if the destination is occupied by a detective, and if so, skip adding moves to that destination.
				3.Checked if the player has the required tickets for each transport, and only added a move if the player has the tickets.
				4.Added code to handle secret moves for MrX. If the current round is a secret round, and MrX has a secret ticket, then a move using the secret ticket is added.*/
			var singleMoves = new HashSet<Move.SingleMove>();
			for (int destination : setup.graph.adjacentNodes(source)) {
				boolean isSECRET = false;
				boolean sameLocation = false;
				for (Player detective : detectives) {
					if (detective.location() == destination) {
						sameLocation = true;
					}
				}
				if (!sameLocation) {
					for (Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
						if (player.hasAtLeast(t.requiredTicket(), 1)) {
							singleMoves.add(new Move.SingleMove(player.piece(), source, t.requiredTicket(), destination));
							if (t.requiredTicket().equals(Ticket.SECRET)) isSECRET = true;
						}
					}
					if (player.isMrX()) {
						Move.SingleMove mrXMove = new Move.SingleMove(player.piece(), source, Ticket.SECRET, destination);
						singleMoves.add(new Move.SingleMove(player.piece(), source, Ticket.SECRET, destination));
					}
				}
			}


			// TODO return the collection of moves
			return ImmutableSet.copyOf(singleMoves);

		}

		private static Set<Move.DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {
			var doubleMoves = new HashSet<Move.DoubleMove>();
			if (!player.isMrX()) return ImmutableSet.copyOf(doubleMoves);
			Set<Move.SingleMove> firstMoves = makeSingleMoves(setup, detectives, player, source);
			//set the first step as a start, then make the second move.
			for (Move.SingleMove firstMove : firstMoves) {
				Set<Move.SingleMove> secondMoves = makeSingleMoves(setup, detectives, player, firstMove.destination);
				for (Move.SingleMove secondMove : secondMoves) {

					if (firstMove.ticket == secondMove.ticket) {
						if (!player.hasAtLeast(secondMove.ticket, 2))
							continue;//the player hasAtLeast 2 tickets to move back to their original place.
					}//add the first step and the second step as a new step
					var moveInto = new Move.DoubleMove(player.piece(), source,
							firstMove.ticket, firstMove.destination,
							secondMove.ticket, secondMove.destination);
					if (!doubleMoves.contains(moveInto)) {
						doubleMoves.add(moveInto);
					}
				}
			}
			return ImmutableSet.copyOf(doubleMoves);
		}

		@Nonnull
		@Override
		public GameState advance(Move move) {
			if (!moves.contains(move)) throw new IllegalArgumentException("Illegal move: " + move);
			//whether the tickets are double move
			Move.Visitor<Boolean> isDouble = new Move.Visitor<Boolean>() {
				@Override
				public Boolean visit(Move.SingleMove move) {
					return false;
				}

				@Override
				public Boolean visit(Move.DoubleMove move) {
					for (Ticket t : move.tickets()) {
						if (t.equals((Ticket.DOUBLE))) return true;
					}
					return false;
				}
			};
			Boolean isDoubleMove = move.accept(isDouble);
			Move.Visitor<Integer> visitDestination01 = new Move.Visitor<Integer>() {
				@Override
				public Integer visit(Move.SingleMove move) {
					return move.destination;
				}

				@Override
				public Integer visit(Move.DoubleMove move) {
					return move.destination1;
				}
			};
			Move.Visitor<Integer> visitDestination02 = new Move.Visitor<Integer>() {
				@Override
				public Integer visit(Move.SingleMove move) {
					return move.destination;
				}

				@Override
				public Integer visit(Move.DoubleMove move) {
					return move.destination2;
				}
			};
			Move.Visitor<Ticket> visitTicket01 = new Move.Visitor<Ticket>() {
				@Override
				public Ticket visit(Move.SingleMove move) {
					return move.ticket;
				}

				@Override
				public Ticket visit(Move.DoubleMove move) {
					return move.ticket1;
				}
			};
			Move.Visitor<Ticket> visitTicket02 = new Move.Visitor<Ticket>() {
				@Override
				public Ticket visit(Move.SingleMove move) {
					return move.ticket;
				}

				@Override
				public Ticket visit(Move.DoubleMove move) {
					return move.ticket2;
				}
			};
			// Create new Player and Player list objects
			int dest1 = move.accept(visitDestination01);
			int dest2 = move.accept(visitDestination02);
			Ticket ticket1 = move.accept(visitTicket01);
			Ticket ticket2 = move.accept(visitTicket02);
			Player newMrx = new Player(MrX.MRX, mrX.tickets(), mrX.location());
			List<Player> newDetective = new ArrayList<>(detectives);
			ImmutableList<LogEntry> newMrXTravelLog;
			//implemented section
			if (!moves.contains(move)) throw new IllegalArgumentException("Illegal move :" + move +"size" +remaining.size()
			+remaining.size()+ "mover" + move.commencedBy() + "Logsize" +mrXTravelLog.size() + "d1=" +dest1 +"d2"
					+"/n"+detectives.iterator().next().location()+moves.toString());//just a point

			if (move.commencedBy().isDetective()) {
				//newDetective = new ArrayList<>();
				for (Player player : newDetective) {
					if (player.piece() == move.commencedBy()) {
						player=player.at(dest1);
						player=player.use(ticket1);
						newMrx=newMrx.give(ticket1);
					}
				}

			} else {
				if (!isDoubleMove){
					newMrx = newMrx.use(ticket1);
					newMrx = newMrx.at(dest1);
				}else {
					newMrx = newMrx.at(dest2);
					newMrx = newMrx.use(ticket1);
					newMrx= newMrx.use(ticket2);
				}
			}
			var newRemaining = newRemaining(move);                                                                //update the remaining players
			newMrXTravelLog = newMrXLog(move, ticket1, ticket2, dest1, dest2, isDoubleMove);                        //update the move of mrX
			return new MyGameState(setup,newMrx,newRemaining,newMrXTravelLog, copyOf(newDetective));

		}

		@NotNull
		@Override
		public ImmutableSet<Move> getAvailableMove() {
			return null;
		}

//		@NotNull
//		@Override
//		public ImmutableSet<Move> getAvailableMove() {
//			return null;
//		}

		//implement newMrXLog and newRemaining. The newRemaining and the newMrXLog is for update the move of remaining players and mrX as stated.
		private ImmutableSet<Piece> newRemaining(Move move) {
			Set<Piece> updatedRemaining = new HashSet<>();
			//After mrX moves, add all the left detectives into the remaining
			if (move.commencedBy().isMrX()) {
				for(Player detective: detectives){
					updatedRemaining.add(detective.piece());
				}
				if (remaining.size()==1){
					return ImmutableSet.of(MrX.MRX);
				}
				else {
					//add those who still have tickets into remaining
					remaining.forEach(piece -> {
						Player player = Objects.requireNonNull(getPlayer(piece));
						if (piece != move.commencedBy())
						if (!player.has(Ticket.TAXI)) {
							if (!player.has(Ticket.BUS)) {
								player.has(Ticket.UNDERGROUND);
							}
						}
						updatedRemaining.add(piece);

					});
				}
				if (remaining.isEmpty()) {
					if (mrXTravelLog.size() == setup.moves.size()) {
						remaining = ImmutableSet.of();
					}
					else {
						remaining = ImmutableSet.of(MrX.MRX);
					}}


			}


			return ImmutableSet.copyOf(updatedRemaining);
		}

		private ImmutableList<LogEntry> newMrXLog(Move move, Ticket ticket1, Ticket ticket2, int dest1, int dest2, Boolean isDoubleMove) {
			if(move.commencedBy()!= MrX.MRX) return mrXTravelLog;
			List<LogEntry> updatedMrXLog = new ArrayList<>(mrXTravelLog);
			if(!isDoubleMove){
				if(setup.moves.get(updatedMrXLog.size())) {
					updatedMrXLog.add(LogEntry.reveal(ticket1,dest1));
				}else updatedMrXLog.add(LogEntry.hidden(ticket1));
			}else{
				if (setup.moves.get(updatedMrXLog.size())) {
					updatedMrXLog.add(LogEntry.reveal(ticket1, dest1));
				}else updatedMrXLog.add(LogEntry.hidden(ticket1));
				if (setup.moves.get(updatedMrXLog.size()+1)){
					updatedMrXLog.add(LogEntry.reveal(ticket2,dest2));
				}else  updatedMrXLog.add(LogEntry.hidden(ticket2));
			}
			return copyOf(updatedMrXLog);
		}

		@Nonnull @Override public ImmutableSet<Piece> getWinner() {

			Set<Piece> winnerSet = new HashSet<>();
			var detectiveHaveTickets = false;
			detectives.forEach(d -> winnerSet.add(d.piece()));

			for (Player detective : detectives) {
				// D win, Mrx have same location with D:
				if (detective.location() == mrX.location()) {
					return ImmutableSet.copyOf(winnerSet);
				}
			}
			if (getOneAvailableMoves(mrX.piece()).isEmpty())
				return ImmutableSet.copyOf(winnerSet);
			for (Player d : detectives) {                                                                        //Mrx win, detectives doesn't have the ticket to move
				for (Ticket ticket : Ticket.values()) {
					if (d.has(ticket)) detectiveHaveTickets = true;
				}
			}
			if (!detectiveHaveTickets) return ImmutableSet.of(mrX.piece());
			if (mrXTravelLog.size() == setup.moves.size() && remaining.contains(MrX.MRX))                        //Mrx win ,and mrX has done his last turn
				return ImmutableSet.of(mrX.piece());
			if (setup.moves.isEmpty()) return ImmutableSet.of(MrX.MRX);
//			if (moves.isEmpty() && remaining.contains(MrX.MRX)) return ImmutableSet.copyOf(winnerSet);
//			if (moves.isEmpty() && (remaining.size() == detectives.size())) return ImmutableSet.of(mrX.piece());
			return ImmutableSet.of();
		}

	}
}
