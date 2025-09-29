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
let loadingOverlayTitleElement = null;
let loadingOverlayMessageElement = null;
let locationContentElement = null;
let cafeLocationElement = null;
let myLocationElement = null;
let distanceInfoElement = null;
let hasCafeCoords = false;
let hasUserCoords = false;
let overlayState = 'hidden';

const OverlayStates = Object.freeze({
    HIDDEN: 'hidden',
    INITIAL: 'initial',
    STAY: 'stay',
    ERROR: 'error'
});

const overlayCopy = {
    [OverlayStates.INITIAL]: {
        title: '위치 정보를 불러오는 중…',
        message: '내 위치와 카페 위치를 확인하고 있어요. 잠시만 기다려 주세요!'
    },
    [OverlayStates.STAY]: {
        title: '핸드드립 중…',
        message: '따끈한 화면을 내리는 중이에요 ☕️'
    },
    [OverlayStates.ERROR]: {
        title: '위치 정보를 가져올 수 없어요',
        message: '브라우저에서 위치 접근 권한을 허용했는지 확인해 주세요.'
    }
};

const DISTANCE_THRESHOLD = 5000; // meters
const STAY_DURATION = 5000; // milliseconds
const THRESHOLD_BUFFER = 50; // meters of tolerance when exiting the zone

function getLoadingOverlay() {
    if (!loadingOverlay) {
        loadingOverlay = document.getElementById('loadingOverlay');
    }
    return loadingOverlay;
}

function showLoadingOverlay(state = OverlayStates.STAY) {
    const overlay = getLoadingOverlay();
    if (!overlay) {
        return;
    }
    const nextState = overlayCopy[state] ? state : OverlayStates.STAY;
    if (overlayState === nextState && overlay.classList.contains('is-visible')) {
        return;
    }
    const copy = overlayCopy[nextState];
    if (loadingOverlayTitleElement && copy.title) {
        loadingOverlayTitleElement.textContent = copy.title;
    }
    if (loadingOverlayMessageElement && copy.message) {
        loadingOverlayMessageElement.textContent = copy.message;
    }
    overlay.dataset.state = nextState;
    overlay.style.display = 'flex';
    overlay.classList.add('is-visible');
    overlay.setAttribute('aria-hidden', 'false');
    overlayState = nextState;
}

function hideLoadingOverlay() {
    const overlay = getLoadingOverlay();
    if (!overlay) {
        return;
    }
    overlay.classList.remove('is-visible');
    overlay.setAttribute('aria-hidden', 'true');
    overlay.style.display = 'none';
    overlay.removeAttribute('data-state');
    overlayState = OverlayStates.HIDDEN;
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
                showLoadingOverlay(OverlayStates.ERROR);
            }
        });
    }

    if (navigator.geolocation) {
        watchId = navigator.geolocation.watchPosition(updateLocation, handleError, {enableHighAccuracy:true});
    } else {
        document.getElementById('myLocation').textContent = '이 브라우저는 위치 정보를 지원하지 않습니다.';
        showLoadingOverlay(OverlayStates.ERROR);
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
    showLoadingOverlay(OverlayStates.ERROR);
}

function evaluateDistanceAndCertification() {
    if (!hasCafeCoords || !hasUserCoords || !lastKnownCoords || typeof cafeLat !== 'number' || typeof cafeLng !== 'number') {
        if (overlayState !== OverlayStates.ERROR) {
            showLoadingOverlay(OverlayStates.INITIAL);
        }
        return;
    }

    if (overlayState === OverlayStates.INITIAL) {
        hideLoadingOverlay();
    }

    const { lat, lng } = lastKnownCoords;
    const dist = calculateDistance(lat, lng, cafeLat, cafeLng);
    const roundedDist = Math.round(dist); // keep comparisons aligned with the value shown to the user

    if (distanceInfoElement) {
        distanceInfoElement.textContent = '카페까지 거리: ' + roundedDist + 'm';
    }

    if (certified) {
        return;
    }

    if (roundedDist <= DISTANCE_THRESHOLD) {
        if (!isWithinThreshold) {
            isWithinThreshold = true;
            showLoadingOverlay(OverlayStates.STAY);
            if (stayTimer) {
                clearTimeout(stayTimer);
            }
            stayTimer = setTimeout(() => {
                certified = true;
                onCertificationSuccess();
            }, STAY_DURATION);
        } else if (overlayState !== OverlayStates.STAY) {
            showLoadingOverlay(OverlayStates.STAY);
        }
        return;
    }

    if (isWithinThreshold && roundedDist <= DISTANCE_THRESHOLD + THRESHOLD_BUFFER) {
        if (overlayState !== OverlayStates.STAY) {
            showLoadingOverlay(OverlayStates.STAY);
        }
        return;
    }

    if (isWithinThreshold) {
        isWithinThreshold = false;
    }
    if (overlayState !== OverlayStates.ERROR) {
        hideLoadingOverlay();
    }
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

function initializeLocationPage() {
    if (loadingOverlay) {
        // 이미 초기화가 수행된 경우 다시 실행하지 않는다.
        return;
    }

    loadingOverlay = document.getElementById('loadingOverlay');
    loadingOverlayTitleElement = document.getElementById('loadingOverlayTitle');
    loadingOverlayMessageElement = document.getElementById('loadingOverlayMessage');
    cafeLocationElement = document.getElementById('cafeLocation');
    myLocationElement = document.getElementById('myLocation');
    distanceInfoElement = document.getElementById('distanceInfo');
    locationContentElement = document.getElementById('locationContent');

    // 필수 엘리먼트가 누락된 경우에는 더 이상 진행하지 않는다.
    if (!loadingOverlay || !loadingOverlayTitleElement || !loadingOverlayMessageElement) {
        console.warn('위치 인증 로딩 오버레이 요소를 찾을 수 없습니다.');
        return;
    }

    showLoadingOverlay(OverlayStates.INITIAL);
    loadKakao();
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initializeLocationPage);
} else {
    initializeLocationPage();
}
