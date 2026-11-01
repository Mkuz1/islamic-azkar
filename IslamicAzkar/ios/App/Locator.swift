import CoreLocation
import Foundation

/// Finds the phone's approximate position once (city-level accuracy is enough for prayer times)
/// and names the town in Arabic and English.
@MainActor
final class Locator: NSObject, CLLocationManagerDelegate {
    private let manager = CLLocationManager()
    private var continuation: CheckedContinuation<CLLocation?, Never>?

    func find() async -> Place? {
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyKilometer
        let location: CLLocation? = await withCheckedContinuation { c in
            continuation = c
            switch manager.authorizationStatus {
            case .notDetermined: manager.requestWhenInUseAuthorization()
            case .denied, .restricted: finish(nil)
            default: manager.requestLocation()
            }
        }
        guard let location else { return nil }
        let ar = await name(location, "ar")
        let en = await name(location, "en")
        return Place(nameAr: ar ?? en ?? "", name: en ?? ar ?? "", lat: location.coordinate.latitude,
                     lng: location.coordinate.longitude, tz: TimeZone.current.identifier)
    }

    private func finish(_ l: CLLocation?) {
        continuation?.resume(returning: l)
        continuation = nil
    }

    private func name(_ l: CLLocation, _ lang: String) async -> String? {
        let marks = try? await CLGeocoder().reverseGeocodeLocation(l, preferredLocale: Locale(identifier: lang))
        let p = marks?.first
        return p?.locality ?? p?.subAdministrativeArea ?? p?.administrativeArea
    }

    nonisolated func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        Task { @MainActor in
            guard continuation != nil else { return }
            switch manager.authorizationStatus {
            case .authorizedWhenInUse, .authorizedAlways: manager.requestLocation()
            case .denied, .restricted: finish(nil)
            default: break
            }
        }
    }

    nonisolated func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        let l = locations.last
        Task { @MainActor in finish(l) }
    }

    nonisolated func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        Task { @MainActor in finish(nil) }
    }
}
