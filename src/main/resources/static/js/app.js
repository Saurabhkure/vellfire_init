/**
 * Vellfire — Landing page geolocation handler
 */

function requestLocation() {
    const locateBtn       = document.getElementById('locateBtn');
    const locatingSpinner = document.getElementById('locatingSpinner');
    const locationError   = document.getElementById('jsLocationError');

    if (locationError) locationError.style.display = 'none';
    if (locateBtn)     locateBtn.style.display = 'none';
    if (locatingSpinner) locatingSpinner.style.display = 'flex';

    if (!navigator.geolocation) {
        showLocationError('Your browser does not support geolocation. Please use a modern browser.');
        return;
    }

    navigator.geolocation.getCurrentPosition(
        function onSuccess(position) {
            var lat = position.coords.latitude;
            var lng = position.coords.longitude;

            if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
                showLocationError('Your browser returned invalid coordinates. Please try again.');
                return;
            }

            document.getElementById('latInput').value = lat;
            document.getElementById('lngInput').value = lng;
            document.getElementById('searchForm').submit();
        },
        function onError(err) {
            switch (err.code) {
                case err.PERMISSION_DENIED:
                    showLocationError(
                        'Location permission was denied. ' +
                        'Please enable location access in your browser settings and try again.'
                    );
                    break;
                case err.POSITION_UNAVAILABLE:
                    showLocationError('Your location could not be determined. Please try again.');
                    break;
                case err.TIMEOUT:
                    showLocationError('Location request timed out. Please try again.');
                    break;
                default:
                    showLocationError('An unexpected error occurred while detecting your location.');
            }
        },
        {
            enableHighAccuracy: true,
            timeout: 12000,
            maximumAge: 0
        }
    );
}

function showLocationError(message) {
    var locateBtn       = document.getElementById('locateBtn');
    var locatingSpinner = document.getElementById('locatingSpinner');
    var locationError   = document.getElementById('jsLocationError');

    if (locatingSpinner) locatingSpinner.style.display = 'none';
    if (locateBtn)       locateBtn.style.display = 'flex';
    if (locationError) {
        locationError.textContent = message;
        locationError.style.display = 'block';
    }
}
