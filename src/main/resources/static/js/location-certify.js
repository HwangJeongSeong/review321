let map;
let currentMarker;
let cafeLat, cafeLng;
let watchId;
let stayTimer = null;
let certified = false;
let isWithinThreshold = false;
let cafeMarkerImage;
let userMarkerImage;
let lastKnownCoords = null;

let loadingOverlay = null;
let locationContentElement = null;
let cafeLocationElement = null;
let myLocationElement = null;
let distanceInfoElement = null;
let hasCafeCoords = false;
let hasUserCoords = false;

const DISTANCE_THRESHOLD = 5000; // meters
const STAY_DURATION = 5000; // milliseconds
const THRESHOLD_BUFFER = 50; // meters of tolerance when exiting the zone

function getLoadingOverlay() {
    if (!loadingOverlay) {
        loadingOverlay = document.getElementById('loadingOverlay');
    }
    return loadingOverlay;
}

function showLoadingOverlay() {
    const overlay = getLoadingOverlay();
    if (!overlay || overlay.classList.contains('is-visible')) {
        return;
    }
    overlay.style.display = 'flex';
    overlay.classList.add('is-visible');
    overlay.setAttribute('aria-hidden', 'false');
}

function hideLoadingOverlay() {
    const overlay = getLoadingOverlay();
    if (!overlay) {
        return;
    }
    overlay.classList.remove('is-visible');
    overlay.setAttribute('aria-hidden', 'true');
    overlay.style.display = '';
}

function initMap() {
    const mapContainer = document.getElementById('map');
    const options = { center: new kakao.maps.LatLng(37.5665,126.9780), level:3 };
    map = new kakao.maps.Map(mapContainer, options);

    const markerSize = new kakao.maps.Size(48, 48);
    const markerOffset = new kakao.maps.Point(24, 48);
    cafeMarkerImage = new kakao.maps.MarkerImage('/images/location_certify/cafe-marker.png', markerSize, {offset: markerOffset});
    userMarkerImage = new kakao.maps.MarkerImage('/images/location_certify/user-marker.png', markerSize, {offset: markerOffset});

    const places = new kakao.maps.services.Places();
    if (cafeAddress) {
        places.keywordSearch(cafeAddress, function(result, status){
            if (status === kakao.maps.services.Status.OK) {
                const first = result[0];
                cafeLat = parseFloat(first.y);
                cafeLng = parseFloat(first.x);
                const pos = new kakao.maps.LatLng(cafeLat, cafeLng);
                hasCafeCoords = true;
                new kakao.maps.Marker({map:map, position:pos, image: cafeMarkerImage});
                map.setCenter(pos);
                if (cafeLocationElement) {
                    cafeLocationElement.textContent = '카페 위치: ' + cafeAddress;
                }
                evaluateDistanceAndCertification();
            } else {
                hasCafeCoords = false;
                hideLoadingOverlay();
            }
        });
    }

    if (navigator.geolocation) {
        watchId = navigator.geolocation.watchPosition(updateLocation, handleError, {enableHighAccuracy:true});
    } else {
        document.getElementById('myLocation').textContent = '이 브라우저는 위치 정보를 지원하지 않습니다.';
        hideLoadingOverlay();
    }
}

function updateLocation(position) {
    const lat = position.coords.latitude;
    const lng = position.coords.longitude;
    const pos = new kakao.maps.LatLng(lat, lng);
    if (currentMarker) {
        currentMarker.setMap(null);
    }
    currentMarker = new kakao.maps.Marker({map:map, position:pos, image: userMarkerImage});

    if (myLocationElement) {
        myLocationElement.textContent = '내 위치: ' + lat.toFixed(6) + ', ' + lng.toFixed(6);
    }

    lastKnownCoords = { lat, lng };
    hasUserCoords = true;
    evaluateDistanceAndCertification();
}

function handleError(err) {
    console.error(err);
    if (myLocationElement) {
        myLocationElement.textContent = '위치를 가져올 수 없습니다.';
    }
    hideLoadingOverlay();
}

function evaluateDistanceAndCertification() {
    if (!hasCafeCoords || !hasUserCoords || !lastKnownCoords || typeof cafeLat !== 'number' || typeof cafeLng !== 'number') {
        showLoadingOverlay();
        return;
    }

    const { lat, lng } = lastKnownCoords;
    const dist = calculateDistance(lat, lng, cafeLat, cafeLng);

    if (distanceInfoElement) {
        distanceInfoElement.textContent = '카페까지 거리: ' + Math.round(dist) + 'm';
    }

    if (certified) {
        return;
    }

    if (dist <= DISTANCE_THRESHOLD) {
        if (!isWithinThreshold) {
            isWithinThreshold = true;
        }
        showLoadingOverlay();
        if (!stayTimer) {
            stayTimer = setTimeout(() => {
                certified = true;
                onCertificationSuccess();
            }, STAY_DURATION);
        }
        return;
    }

    if (isWithinThreshold && dist <= DISTANCE_THRESHOLD + THRESHOLD_BUFFER) {
        showLoadingOverlay();
        return;
    }

    if (isWithinThreshold) {
        isWithinThreshold = false;
    }
    hideLoadingOverlay();
    if (stayTimer) {
        clearTimeout(stayTimer);
        stayTimer = null;
    }
}

function calculateDistance(lat1, lon1, lat2, lon2) {
    const R = 6371000;
    const toRad = v => v * Math.PI / 180;
    const dLat = toRad(lat2 - lat1);
    const dLon = toRad(lon2 - lon1);
    const a = Math.sin(dLat/2) * Math.sin(dLat/2) +
              Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) *
              Math.sin(dLon/2) * Math.sin(dLon/2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    return R * c;
}

function onCertificationSuccess() {
    stayTimer = null;
    isWithinThreshold = false;
    hideLoadingOverlay();
    alert('위치 인증이 성공했습니다.');
    if (watchId) {
        navigator.geolocation.clearWatch(watchId);
    }
    if (typeof cafeId !== 'undefined') {
        localStorage.setItem('certifiedCafe_' + cafeId, 'true');
        window.location.href = '/cafes/' + cafeId + '/reviews';
    }
}

function loadKakao() {
    if (window.kakao && window.kakao.maps) {
        window.kakao.maps.load(initMap);
    } else {
        setTimeout(loadKakao, 50);
    }
}

document.addEventListener('DOMContentLoaded', () => {
    loadingOverlay = document.getElementById('loadingOverlay');
    cafeLocationElement = document.getElementById('cafeLocation');
    myLocationElement = document.getElementById('myLocation');
    distanceInfoElement = document.getElementById('distanceInfo');
    locationContentElement = document.getElementById('locationContent');
    showLoadingOverlay();
    loadKakao();
});
