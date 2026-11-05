import Foundation
import Shared

/// Pure-Swift row model so SwiftUI can scroll without crossing the KMP bridge per property.
struct FlightCard: Identifiable, Equatable, Hashable {
    let id: String
    let destinationCity: String
    let origin: String
    let destination: String
    let points: Int
    let cabin: String
    let departureDate: String
    let program: String

    init(_ flight: Flight) {
        id = flight.id
        destinationCity = flight.destinationCity
        origin = flight.origin
        destination = flight.destination
        points = Int(flight.points)
        cabin = flight.cabin
        departureDate = flight.departureDate
        program = flight.program
    }
}
