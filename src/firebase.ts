import { initializeApp, getApps, getApp, FirebaseApp } from 'firebase/app';
import {
  getDatabase,
  ref,
  onValue,
  set,
  update,
  remove,
  get,
  Database,
  Unsubscribe as RTDBUnsubscribe
} from 'firebase/database';
import {
  initializeFirestore,
  getFirestore,
  doc,
  collection,
  onSnapshot,
  setDoc,
  updateDoc,
  deleteDoc,
  getDoc,
  getDocs,
  writeBatch,
  Firestore,
  Unsubscribe as FirestoreUnsubscribe
} from 'firebase/firestore';
import {
  getAuth,
  signInWithEmailAndPassword,
  signOut,
  onAuthStateChanged,
  User,
  Auth
} from 'firebase/auth';
import {
  Driver,
  Rider,
  Trip,
  SurgeZone,
  PricingConfig,
  SupportTicket,
  LiveActivityFeedItem,
  VehicleInfo,
  DocumentVerification,
  DeviceTelemetry,
  DriverStatus,
  RiderStatus,
  PaymentMethod,
  PaymentStatus,
  VehicleType,
  TripStatus,
  DriverVerification,
  DriverVerificationStatus,
  DriverAccountStatus,
  PassengerAccountStatus,
  LiveDriverLocation,
  SafetyReport,
  RideRating,
  AdminNotification,
  PayoutRequest,
  OperationalRecommendation,
  WalletTransaction
} from './types';
import { resolvePhysicalAddress, calculateAccurateRouteDistance } from './utils/geoUtils';
import {
  INITIAL_DRIVERS,
  INITIAL_RIDERS,
  INITIAL_TRIPS,
  INITIAL_SURGE_ZONES,
  DEFAULT_PRICING_CONFIGS,
  INITIAL_SUPPORT_TICKETS,
  INITIAL_FEED_ITEMS,
  INITIAL_RIDE_RATINGS,
  INITIAL_ADMIN_NOTIFICATIONS,
  INITIAL_PAYOUT_REQUESTS
} from './mockData';

export interface CustomFirebaseConfig {
  apiKey?: string;
  authDomain?: string;
  databaseURL?: string; // Crucial for Realtime Database
  projectId?: string;
  storageBucket?: string;
  messagingSenderId?: string;
  appId?: string;
}

const LOCAL_STORAGE_KEY = 'drigo_firebase_custom_config';

export function getSavedFirebaseConfig(): CustomFirebaseConfig | null {
  try {
    const raw = localStorage.getItem(LOCAL_STORAGE_KEY);
    if (raw) {
      return JSON.parse(raw);
    }
  } catch (err) {
    console.error('Failed to parse saved Firebase config', err);
  }
  return null;
}

export function saveFirebaseConfig(config: CustomFirebaseConfig) {
  try {
    localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(config));
  } catch (err) {
    console.error('Failed to save Firebase config', err);
  }
}

export function clearFirebaseConfig() {
  localStorage.removeItem(LOCAL_STORAGE_KEY);
}

// Default config matching user's live Firebase project
const demoConfig: CustomFirebaseConfig = {
  apiKey: "AIzaSyBypFSH7BN6JuCTmQafTI0W0Gv0NMNMOwE",
  authDomain: "drigo-8b15c.firebaseapp.com",
  databaseURL: "https://drigo-8b15c-default-rtdb.firebaseio.com",
  projectId: "drigo-8b15c",
  storageBucket: "drigo-8b15c.firebasestorage.app",
  messagingSenderId: "250625869331",
  appId: "1:250625869331:android:a9d03bcc51e0e4bbc221e6"
};

let activeApp: FirebaseApp | null = null;
let activeDatabase: Database | null = null;
let activeFirestore: Firestore | null = null;
let activeAuth: Auth | null = null;

export let auth: Auth | null = null;

export function initFirebaseService(customConfig?: CustomFirebaseConfig | null) {
  const savedConfig = getSavedFirebaseConfig();
  const rawConfig = customConfig || savedConfig || {};

  // Guarantee every required field is present with fallback to demoConfig
  const config: Required<CustomFirebaseConfig> = {
    apiKey: (rawConfig.apiKey && rawConfig.apiKey.trim()) || demoConfig.apiKey!,
    authDomain: (rawConfig.authDomain && rawConfig.authDomain.trim()) || (rawConfig.projectId ? `${rawConfig.projectId}.firebaseapp.com` : demoConfig.authDomain!),
    databaseURL: (rawConfig.databaseURL && rawConfig.databaseURL.trim()) || demoConfig.databaseURL!,
    projectId: (rawConfig.projectId && rawConfig.projectId.trim()) || demoConfig.projectId!,
    storageBucket: (rawConfig.storageBucket && rawConfig.storageBucket.trim()) || (rawConfig.projectId ? `${rawConfig.projectId}.firebasestorage.app` : demoConfig.storageBucket!),
    messagingSenderId: (rawConfig.messagingSenderId && rawConfig.messagingSenderId.trim()) || demoConfig.messagingSenderId!,
    appId: (rawConfig.appId && rawConfig.appId.trim()) || demoConfig.appId!
  };

  try {
    const existingApps = getApps();
    if (existingApps.length > 0) {
      const defaultApp = existingApps[0];
      if (defaultApp && defaultApp.options && defaultApp.options.projectId && defaultApp.options.databaseURL) {
        activeApp = defaultApp;
      } else {
        const appName = `drigo-app-${Date.now()}`;
        activeApp = initializeApp(config, appName);
      }
    } else {
      activeApp = initializeApp(config);
    }

    if (activeApp) {
      try {
        activeDatabase = getDatabase(activeApp, config.databaseURL);
      } catch (err) {
        console.warn('Realtime database initialization note:', err);
      }

      try {
        activeFirestore = initializeFirestore(activeApp, {
          experimentalForceLongPolling: true,
          ignoreUndefinedProperties: true
        });
      } catch (err) {
        try {
          activeFirestore = getFirestore(activeApp);
        } catch (err2) {
          console.warn('Firestore initialization note:', err2);
        }
      }

      try {
        activeAuth = getAuth(activeApp);
        auth = activeAuth;
      } catch (err) {
        console.warn('Auth initialization note:', err);
      }
    }
  } catch (error) {
    console.error('Firebase Initialization Warning:', error);
  }

  return { app: activeApp, db: activeDatabase, firestore: activeFirestore, auth: activeAuth };
}

// Initialize on module load
initFirebaseService();

/**
 * Ensures that an admin record exists in both Firestore and Realtime Database
 * to satisfy strict backend security rules
 */
export async function ensureAdminRecordExists(uid: string, email: string) {
  const { db, firestore } = initFirebaseService();
  if (firestore) {
    try {
      const adminRef = doc(firestore, 'admins', uid);
      await setDoc(adminRef, {
        email: email.toLowerCase(),
        role: 'admin',
        isAdmin: true,
        exists: true,
        updatedAt: new Date().toISOString()
      }, { merge: true });
    } catch (e) {
      console.warn('Could not write admin doc in firestore:', e);
    }
  }
  if (db) {
    try {
      await update(ref(db, `admins/${uid}`), {
        email: email.toLowerCase(),
        role: 'admin',
        isAdmin: true,
        exists: true,
        updatedAt: new Date().toISOString()
      });
    } catch (e) {
      console.warn('Could not write admin in RTDB:', e);
    }
  }
}

/**
 * Checks if a user has the administrative role 'admin'
 * Supporting boot-strapped emails, firestore, and RTDB verification
 */
export async function checkUserIsAdmin(uid: string, email: string | null): Promise<boolean> {
  if (!uid) return false;

  // 1. Bootstrapped email checks (Self-healing registration)
  const adminEmails = [
    'naeemullahsilverdale@gmail.com',
    'admin@drigo.app',
    'admin@drigo.pk',
    'alex.sterling@drigo.app'
  ];
  if (email && adminEmails.includes(email.toLowerCase())) {
    await ensureAdminRecordExists(uid, email);
    return true;
  }

  // 2. Database checks
  const { db, firestore } = initFirebaseService();

  if (firestore) {
    try {
      const adminDoc = await getDoc(doc(firestore, 'admins', uid));
      if (adminDoc.exists()) {
        const data = adminDoc.data();
        if (data && (data.role === 'admin' || data.isAdmin === true || data.exists === true)) {
          return true;
        }
      }

      const userDoc = await getDoc(doc(firestore, 'users', uid));
      if (userDoc.exists()) {
        const data = userDoc.data();
        if (data && (data.role === 'admin' || data.userType === 'admin' || data.type === 'admin')) {
          return true;
        }
      }
    } catch (e) {
      console.warn('Firestore admin check note:', e);
    }
  }

  if (db) {
    try {
      const adminSnap = await get(ref(db, `admins/${uid}`));
      if (adminSnap.exists()) {
        const data = adminSnap.val();
        if (data && (data.role === 'admin' || data.isAdmin === true || data.exists === true)) {
          return true;
        }
      }

      const userSnap = await get(ref(db, `users/${uid}`));
      if (userSnap.exists()) {
        const data = userSnap.val();
        if (data && (data.role === 'admin' || data.userType === 'admin' || data.type === 'admin')) {
          return true;
        }
      }
    } catch (e) {
      console.warn('RTDB admin check note:', e);
    }
  }

  return false;
}

export const INITIAL_DRIVER_VERIFICATIONS: DriverVerification[] = [
  {
    id: 'VER-8821-X',
    driverId: 'drv-101',
    driverName: 'Muhammad Tariq',
    fullName: 'Muhammad Tariq',
    profileImage: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80',
    avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80',
    documentId: 'DOC-8821-NC',
    role: 'Driver',
    phone: '+92 300 5550192',
    email: 'tariq.m@drigo.app',
    vehicleMake: 'Honda',
    vehicleModel: 'Civic',
    licensePlate: 'ICT-4920',
    status: 'PENDING',
    submittedAt: '2026-09-01T10:15:00Z',
  },
  {
    id: 'VER-9902-Y',
    driverId: 'drv-102',
    driverName: 'Zubair Ahmed',
    fullName: 'Zubair Ahmed',
    profileImage: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80',
    avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80',
    documentId: 'DOC-9902-DL',
    role: 'Driver',
    phone: '+92 321 8881234',
    email: 'zubair.a@drigo.app',
    vehicleMake: 'Toyota',
    vehicleModel: 'Corolla',
    licensePlate: 'LEB-8831',
    status: 'REVIEW',
    submittedAt: '2026-09-01T12:30:00Z',
  },
  {
    id: 'VER-7734-Z',
    driverId: 'drv-103',
    driverName: 'Usman Farooq',
    fullName: 'Usman Farooq',
    profileImage: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&auto=format&fit=crop&q=80',
    avatar: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&auto=format&fit=crop&q=80',
    documentId: 'DOC-7734-CNIC',
    role: 'Driver',
    phone: '+92 333 4449876',
    email: 'usman.f@drigo.app',
    vehicleMake: 'Suzuki',
    vehicleModel: 'Alto',
    licensePlate: 'RI-1029',
    status: 'PENDING',
    submittedAt: '2026-09-01T14:45:00Z',
  }
];

/**
 * Seed initial sample Drigo data into Realtime Database / Firestore
 */
export async function seedFirebaseDatabase(customConfig?: CustomFirebaseConfig | null) {
  const { db, firestore } = initFirebaseService(customConfig);

  // 1. Realtime Database Seed
  if (db) {
    try {
      const driversObj: Record<string, Driver> = {};
      INITIAL_DRIVERS.forEach(d => { driversObj[d.id] = d; });

      const ridersObj: Record<string, Rider> = {};
      INITIAL_RIDERS.forEach(r => { ridersObj[r.id] = r; });

      const tripsObj: Record<string, Trip> = {};
      INITIAL_TRIPS.forEach(t => { tripsObj[t.id] = t; });

      const surgeObj: Record<string, SurgeZone> = {};
      INITIAL_SURGE_ZONES.forEach(s => { surgeObj[s.id] = s; });

      const pricingObj: Record<string, PricingConfig> = {};
      DEFAULT_PRICING_CONFIGS.forEach(p => { pricingObj[p.vehicleType] = p; });

      const ticketsObj: Record<string, SupportTicket> = {};
      INITIAL_SUPPORT_TICKETS.forEach(tk => { ticketsObj[tk.id] = tk; });

      const verificationsObj: Record<string, DriverVerification> = {};
      INITIAL_DRIVER_VERIFICATIONS.forEach(v => { verificationsObj[v.id] = v; });

      const ratingsObj: Record<string, RideRating> = {};
      INITIAL_RIDE_RATINGS.forEach(rt => { ratingsObj[rt.id] = rt; });

      const notifsObj: Record<string, AdminNotification> = {};
      INITIAL_ADMIN_NOTIFICATIONS.forEach(n => { notifsObj[n.id] = n; });

      const liveLocationsObj: Record<string, LiveDriverLocation> = {};
      INITIAL_DRIVERS.forEach(d => {
        liveLocationsObj[d.id] = {
          driverId: d.id,
          driverName: d.fullName,
          phone: d.phone,
          lat: d.currentLocation.lat,
          lng: d.currentLocation.lng,
          heading: d.currentLocation.heading,
          speedKmh: d.currentLocation.speedKmh,
          updatedAt: 'Just now',
          status: d.status,
          vehicleType: d.vehicle.type,
          licensePlate: d.vehicle.licensePlate
        };
      });

      await set(ref(db, 'drivers'), driversObj);
      await set(ref(db, 'riders'), ridersObj);
      await set(ref(db, 'trips'), tripsObj);
      await set(ref(db, 'surgeZones'), surgeObj);
      await set(ref(db, 'pricingConfigs'), pricingObj);
      await set(ref(db, 'tickets'), ticketsObj);
      await set(ref(db, 'feed'), INITIAL_FEED_ITEMS);
      await set(ref(db, 'driver_verifications'), verificationsObj);
      await set(ref(db, 'ride_ratings'), ratingsObj);
      await set(ref(db, 'admin_notifications'), notifsObj);
      await set(ref(db, 'live_driver_locations'), liveLocationsObj);

      console.log('Firebase Realtime Database successfully seeded!');
    } catch (error) {
      console.warn('Seeding Realtime Database note:', error);
    }
  }

  // 2. Firestore Seed
  if (firestore) {
    try {
      for (const d of INITIAL_DRIVERS) {
        await setDoc(doc(firestore, 'drivers', d.id), d);
      }
      for (const r of INITIAL_RIDERS) {
        await setDoc(doc(firestore, 'riders', r.id), r);
      }
      for (const t of INITIAL_TRIPS) {
        await setDoc(doc(firestore, 'trips', t.id), t);
      }
      for (const v of INITIAL_DRIVER_VERIFICATIONS) {
        await setDoc(doc(firestore, 'driver_verifications', v.id), v);
      }
      for (const rt of INITIAL_RIDE_RATINGS) {
        await setDoc(doc(firestore, 'ride_ratings', rt.id), rt);
      }
      for (const n of INITIAL_ADMIN_NOTIFICATIONS) {
        await setDoc(doc(firestore, 'admin_notifications', n.id), n);
      }
      for (const d of INITIAL_DRIVERS) {
        const locItem: LiveDriverLocation = {
          driverId: d.id,
          driverName: d.fullName,
          phone: d.phone,
          lat: d.currentLocation.lat,
          lng: d.currentLocation.lng,
          heading: d.currentLocation.heading,
          speedKmh: d.currentLocation.speedKmh,
          updatedAt: 'Just now',
          status: d.status,
          vehicleType: d.vehicle.type,
          licensePlate: d.vehicle.licensePlate
        };
        await setDoc(doc(firestore, 'live_driver_locations', d.id), locItem);
      }
      console.log('Firebase Firestore successfully seeded!');
    } catch (error) {
      console.warn('Seeding Firestore note:', error);
    }
  }
}

/**
 * Helper: Convert docType to human readable Title
 */
export function docTypeToTitle(docType: string): string {
  const dt = String(docType || '').toUpperCase();
  if (dt.includes('CNIC_FRONT')) return 'CNIC / National ID (Front)';
  if (dt.includes('CNIC_BACK')) return 'CNIC / National ID (Back)';
  if (dt.includes('DRIVER_PHOTO') || dt.includes('PROFILE')) return 'Driver Profile Photo';
  if (dt.includes('LICENSE_FRONT')) return 'Driving License (Front)';
  if (dt.includes('LICENSE_BACK')) return 'Driving License (Back)';
  if (dt.includes('VEHICLE_FRONT')) return 'Vehicle (Front View)';
  if (dt.includes('VEHICLE_BACK')) return 'Vehicle (Back View)';
  if (dt.includes('VEHICLE_SIDE')) return 'Vehicle (Side View)';
  if (dt.includes('VEHICLE_REGISTRATION') || dt.includes('REGISTRATION')) return 'Vehicle Registration Card / Book';
  if (dt.includes('ADDITIONAL')) return 'Additional Verification Document';
  return String(docType || '').replace(/_/g, ' ');
}

/**
 * Helper: Extract Google Drive File ID from URL or String
 */
export function extractGoogleDriveFileId(urlOrId: string): string | null {
  if (!urlOrId || typeof urlOrId !== 'string') return null;
  const str = urlOrId.trim();
  if (/^[a-zA-Z0-9_-]{25,}$/.test(str) && !str.includes('/') && !str.includes('http')) {
    return str;
  }
  const match = str.match(/\/file\/d\/([a-zA-Z0-9_-]+)/) || str.match(/id=([a-zA-Z0-9_-]+)/);
  return match ? match[1] : null;
}

/**
 * Helper: Format Image URL for direct embedding in <img> tag and Google Drive view link
 */
export function formatDocImageUrl(rawUrl: string, googleDriveFileId?: string): { fileUrl: string; driveWebLink?: string } {
  if (!rawUrl && !googleDriveFileId) {
    return { fileUrl: '' };
  }
  const fileId = googleDriveFileId || extractGoogleDriveFileId(rawUrl);
  if (fileId) {
    return {
      fileUrl: `https://lh3.googleusercontent.com/d/${fileId}`,
      driveWebLink: `https://drive.google.com/file/d/${fileId}/view?usp=drivesdk`
    };
  }
  return { fileUrl: rawUrl, driveWebLink: rawUrl };
}

/**
 * Helper: Parse raw document item from Firebase Realtime Database
 */
export function parseRawDocItem(docObj: any, defaultKey: string, index: number): DocumentVerification | null {
  if (!docObj) return null;

  if (typeof docObj === 'string') {
    const formatted = formatDocImageUrl(docObj);
    if (!formatted.fileUrl) return null;
    return {
      id: defaultKey || `doc_${index}`,
      type: defaultKey,
      docType: defaultKey,
      title: docTypeToTitle(defaultKey),
      documentNumber: `DOC-${index + 101}`,
      issueDate: '2024-01-01',
      expiryDate: '2028-01-01',
      status: 'pending',
      fileUrl: formatted.fileUrl,
      driveWebLink: formatted.driveWebLink
    };
  }

  if (typeof docObj !== 'object') return null;

  const rawUrl = docObj.fileUrl || docObj.driveFileUrl || docObj.googleDriveWebViewLink || docObj.url || docObj.uri || '';
  const driveId = docObj.googleDriveFileId || docObj.fileId;
  const formatted = formatDocImageUrl(rawUrl, driveId);

  const docType = docObj.docType || docObj.type || docObj.category || defaultKey || `doc_${index}`;
  const rawStatus = (docObj.status || docObj.verificationStatus || 'pending').toString().toLowerCase();

  let status: DocumentVerification['status'] = 'pending';
  if (rawStatus.includes('verif') || rawStatus.includes('approv')) {
    status = 'verified';
  } else if (rawStatus.includes('reject')) {
    status = 'rejected';
  }

  const title = docObj.title || docTypeToTitle(docType) || `Document ${index + 1}`;

  return {
    id: docObj.id || docType || `doc_${index}`,
    type: docType,
    docType,
    category: docObj.category,
    title,
    documentNumber: docObj.documentNumber || docObj.fileName || `DOC-${index + 101}`,
    issueDate: docObj.uploadedAt ? new Date(docObj.uploadedAt).toISOString().split('T')[0] : '2024-01-01',
    expiryDate: '2028-01-01',
    status,
    fileUrl: formatted.fileUrl || rawUrl,
    driveWebLink: formatted.driveWebLink || docObj.googleDriveWebViewLink || rawUrl,
    rejectionReason: docObj.rejectionReason || ''
  };
}

/**
 * Helper: Extract all driver verification documents from any Firebase user / driver object
 */
export function extractDriverDocuments(val: any): DocumentVerification[] {
  const docsList: DocumentVerification[] = [];
  const seenKeys = new Set<string>();

  const addDoc = (d: DocumentVerification | null) => {
    if (!d || !d.fileUrl) return;
    const key = d.docType || d.type || d.id;
    if (seenKeys.has(key)) return;
    seenKeys.add(key);
    docsList.push(d);
  };

  if (!val || typeof val !== 'object') return docsList;

  // 1. Check val.documents (Array or Object)
  if (val.documents) {
    if (Array.isArray(val.documents)) {
      val.documents.forEach((d: any, idx: number) => {
        addDoc(parseRawDocItem(d, d?.docType || `doc_${idx}`, idx));
      });
    } else if (typeof val.documents === 'object') {
      Object.entries(val.documents).forEach(([key, d]: [string, any], idx: number) => {
        addDoc(parseRawDocItem(d, key, idx));
      });
    }
  }

  // 2. Check val.driverVerification.documents or val.driver_verifications.documents
  const verifObj = val.driverVerification || val.driver_verification || val.driver_verifications;
  if (verifObj && verifObj.documents) {
    if (Array.isArray(verifObj.documents)) {
      verifObj.documents.forEach((d: any, idx: number) => {
        addDoc(parseRawDocItem(d, d?.docType || `doc_${idx}`, idx));
      });
    } else if (typeof verifObj.documents === 'object') {
      Object.entries(verifObj.documents).forEach(([key, d]: [string, any], idx: number) => {
        addDoc(parseRawDocItem(d, key, idx));
      });
    }
  }

  // 3. Fallback top-level URIs
  const topLevelUris: Array<{ key: string; uri: any }> = [
    { key: 'DRIVER_PHOTO', uri: val.driverPhotoUri || val.profilePhotoUri || verifObj?.driverPhotoUri },
    { key: 'CNIC_FRONT', uri: val.cnicFrontUri || val.cnicFront || verifObj?.cnicFrontUri },
    { key: 'CNIC_BACK', uri: val.cnicBackUri || val.cnicBack || verifObj?.cnicBackUri },
    { key: 'LICENSE_FRONT', uri: val.drivingLicenseFrontUri || val.licenseFrontUri || verifObj?.drivingLicenseFrontUri },
    { key: 'LICENSE_BACK', uri: val.drivingLicenseBackUri || val.licenseBackUri || verifObj?.drivingLicenseBackUri },
    { key: 'VEHICLE_FRONT', uri: val.vehicleFrontUri || val.vehiclePictureUri || verifObj?.vehicleFrontUri },
    { key: 'VEHICLE_BACK', uri: val.vehicleBackUri || verifObj?.vehicleBackUri },
    { key: 'VEHICLE_SIDE', uri: val.vehicleSideUri || verifObj?.vehicleSideUri },
    { key: 'VEHICLE_REGISTRATION', uri: val.vehicleRegistrationDocUri || val.vehicleCardDocFrontUri || verifObj?.vehicleRegistrationDocUri },
    { key: 'ADDITIONAL_DOC', uri: val.additionalDocUri || verifObj?.additionalDocUri }
  ];

  topLevelUris.forEach(({ key, uri }, idx) => {
    if (uri && typeof uri === 'string') {
      addDoc(parseRawDocItem(uri, key, idx));
    }
  });

  return docsList;
}

/**
 * Safely and robustly extracts a numerical wallet balance from any schema representation
 * in Firebase Realtime Database or Firestore (numbers, strings, nested objects, alternate keys).
 */
export function extractWalletBalance(val: any): number {
  if (val === undefined || val === null) return 0;
  if (typeof val === 'number' && !isNaN(val)) return val;
  if (typeof val === 'string') {
    const cleaned = val.replace(/[^0-9.-]+/g, '');
    const num = parseFloat(cleaned);
    return !isNaN(num) ? num : 0;
  }
  if (typeof val === 'object') {
    const candidates = [
      val.walletBalance,
      val.balance,
      val.wallet_balance,
      val.amount,
      val.currentBalance,
      val.walletAmount,
      val.totalBalance,
      val.credits,
      val.account_balance,
      val.wallet,
      val.driverWallet,
      val.passengerWallet,
      val.wallet_details,
    ];
    for (const cand of candidates) {
      if (cand !== undefined && cand !== null) {
        if (typeof cand === 'number' && !isNaN(cand)) return cand;
        if (typeof cand === 'string') {
          const cleaned = cand.replace(/[^0-9.-]+/g, '');
          const num = parseFloat(cleaned);
          if (!isNaN(num)) return num;
        }
        if (typeof cand === 'object') {
          const nested = extractWalletBalance(cand);
          if (!isNaN(nested) && nested !== 0) return nested;
        }
      }
    }
  }
  return 0;
}

/**
 * Safely extracts the number of completed rides/trips for a driver or rider
 * from any schema in Firebase Realtime Database or Firestore.
 */
export function extractTotalTrips(val: any): number {
  if (val === undefined || val === null) return 0;
  if (typeof val === 'number' && !isNaN(val)) return val;
  if (typeof val === 'string') {
    const cleaned = val.replace(/[^0-9]/g, '');
    const num = parseInt(cleaned, 10);
    return !isNaN(num) ? num : 0;
  }
  if (typeof val === 'object') {
    const directKeys = [
      val.completedRides,
      val.completedTrips,
      val.completed_rides,
      val.completed_trips,
      val.totalCompletedRides,
      val.totalCompletedTrips,
      val.ridesCompleted,
      val.tripsCompleted,
      val.totalTrips,
      val.totalRides,
      val.total_trips,
      val.total_rides,
      val.ridesCount,
      val.tripsCount,
      val.rides_count,
      val.trips_count,
      val.rideCount,
      val.tripCount,
      val.completedRidesCount,
      val.completedTripsCount,
      val.stats?.completedRides,
      val.stats?.completedTrips,
      val.stats?.totalTrips,
      val.stats?.totalRides,
      val.driverStats?.completedRides,
      val.driverStats?.completedTrips,
      val.driverStats?.totalTrips,
      val.driverStats?.totalRides,
      val.passengerStats?.completedRides,
      val.passengerStats?.totalRides,
      val.driver_stats?.completedRides,
      val.driver_stats?.completedTrips,
      val.driver_stats?.totalTrips,
      val.driver_stats?.totalRides,
    ];

    for (const cand of directKeys) {
      if (cand !== undefined && cand !== null) {
        if (typeof cand === 'number' && !isNaN(cand)) return cand;
        if (typeof cand === 'string') {
          const cleaned = cand.replace(/[^0-9]/g, '');
          const num = parseInt(cleaned, 10);
          if (!isNaN(num)) return num;
        }
      }
    }

    // Check collections/arrays inside the user object
    const collectionKeys = [
      val.completed_rides,
      val.completed_trips,
      val.rides,
      val.trips,
      val.history,
      val.pastRides,
      val.past_rides,
      val.myRides,
      val.my_rides,
    ];
    for (const coll of collectionKeys) {
      if (coll && typeof coll === 'object') {
        if (Array.isArray(coll)) return coll.length;
        const count = Object.keys(coll).length;
        if (count > 0) return count;
      }
    }
  }
  return 0;
}

/**
 * Normalizes any Firebase user object (from /users, /drivers, or /riders)
 * into a standard Driver or Rider object for the admin panel.
 */
export function normalizeFirebaseUser(key: string, val: any): { driver?: Driver; rider?: Rider; rawRole: string } {
  if (!val || typeof val !== 'object') {
    return { rawRole: 'unknown' };
  }

  const roleStr = String(val.role || val.userType || val.type || val.user_type || val.accountType || '').toLowerCase();
  const isDriver = roleStr.includes('driver') || Boolean(val.vehicle) || Boolean(val.licenseNumber) || Boolean(val.vehicleDetails) || Boolean(val.carMake) || Boolean(val.isDriver) || Boolean(val.documents) || Boolean(val.driverVerification);

  const fullName = val.fullName || val.name || val.userName || val.user_name || (val.firstName ? `${val.firstName} ${val.lastName || ''}`.trim() : `User ${key.slice(0, 6)}`);
  const phone = val.phone || val.phoneNumber || val.phone_number || val.mobile || val.contact || 'N/A';
  const email = val.email || `${key.slice(0, 8)}@drigo.app`;
  const avatar = val.avatar || val.profileImage || val.photoUrl || val.photoURL || `https://images.unsplash.com/photo-${isDriver ? '1534528741775-53994a69daeb' : '1507003211169-0a1dd7228f2d'}?w=150&auto=format&fit=crop&q=80`;
  const rating = typeof val.rating === 'number' ? val.rating : 4.9;
  const totalTrips = extractTotalTrips(val);
  const walletBalance = extractWalletBalance(val);

  const parsedDocs = extractDriverDocuments(val);

  // Derive driver verificationStatus strictly: PENDING | APPROVED | REJECTED
  let verificationStatusVal: DriverVerificationStatus = 'APPROVED';
  if (val.verificationStatus) {
    const rawV = String(val.verificationStatus).toUpperCase();
    if (rawV.includes('PEND')) verificationStatusVal = 'PENDING';
    else if (rawV.includes('REJ')) verificationStatusVal = 'REJECTED';
    else verificationStatusVal = 'APPROVED';
  } else if (parsedDocs.some(d => d.status === 'rejected')) {
    verificationStatusVal = 'REJECTED';
  } else if (parsedDocs.some(d => d.status === 'pending') || val.isApproved === false || val.isPending === true || val.status === 'pending_verification' || val.status === 'pending') {
    verificationStatusVal = 'PENDING';
  } else {
    verificationStatusVal = 'APPROVED';
  }

  // Derive driver accountStatus strictly: PENDING_REVIEW | ACTIVE | ONLINE | ON_TRIP | SUSPENDED | FLAGGED
  let driverAccountStatusVal: DriverAccountStatus = 'ACTIVE';
  const rawAccountStatus = String(val.accountStatus || '').toUpperCase();
  const rawStatusLower = String(val.status || '').toLowerCase();

  if (rawAccountStatus === 'SUSPENDED' || rawStatusLower === 'suspended') {
    driverAccountStatusVal = 'SUSPENDED';
  } else if (rawAccountStatus === 'FLAGGED' || rawStatusLower === 'flagged') {
    driverAccountStatusVal = 'FLAGGED';
  } else if (rawAccountStatus === 'ON_TRIP' || rawStatusLower === 'on_trip') {
    driverAccountStatusVal = verificationStatusVal === 'APPROVED' ? 'ON_TRIP' : 'PENDING_REVIEW';
  } else if (rawAccountStatus === 'ONLINE' || rawStatusLower === 'online' || val.isOnline === true) {
    driverAccountStatusVal = verificationStatusVal === 'APPROVED' ? 'ONLINE' : 'PENDING_REVIEW';
  } else if (rawAccountStatus === 'PENDING_REVIEW' || verificationStatusVal === 'PENDING' || rawStatusLower === 'pending_verification' || rawStatusLower === 'pending') {
    driverAccountStatusVal = 'PENDING_REVIEW';
  } else {
    driverAccountStatusVal = 'ACTIVE';
  }

  // Derive passenger accountStatus strictly: ACTIVE | ON_TRIP | SUSPENDED | FLAGGED | INACTIVE | DEACTIVATED
  let passengerAccountStatusVal: PassengerAccountStatus = 'ACTIVE';
  if (rawAccountStatus === 'SUSPENDED' || rawStatusLower === 'suspended') {
    passengerAccountStatusVal = 'SUSPENDED';
  } else if (rawAccountStatus === 'FLAGGED' || rawStatusLower === 'flagged') {
    passengerAccountStatusVal = 'FLAGGED';
  } else if (rawAccountStatus === 'ON_TRIP' || rawStatusLower === 'on_trip') {
    passengerAccountStatusVal = 'ON_TRIP';
  } else if (rawAccountStatus === 'DEACTIVATED' || rawStatusLower === 'deactivated') {
    passengerAccountStatusVal = 'DEACTIVATED';
  } else if (rawAccountStatus === 'INACTIVE' || rawStatusLower === 'inactive') {
    passengerAccountStatusVal = 'INACTIVE';
  } else {
    passengerAccountStatusVal = 'ACTIVE';
  }

  let status: DriverStatus = 'online';
  if (driverAccountStatusVal === 'SUSPENDED') {
    status = 'suspended';
  } else if (driverAccountStatusVal === 'PENDING_REVIEW' || verificationStatusVal === 'PENDING') {
    status = 'pending_verification';
  } else if (driverAccountStatusVal === 'ON_TRIP') {
    status = 'on_trip';
  } else if (rawStatusLower === 'offline' || val.isOnline === false) {
    status = 'offline';
  } else {
    status = 'online';
  }

  if (isDriver) {
    const vehicle: VehicleInfo = {
      make: val.vehicleCompany || val.vehicle?.make || val.carMake || val.make || 'Toyota',
      model: val.vehicleModel || val.vehicle?.model || val.carModel || val.model || 'Corolla',
      year: val.vehicle?.year || val.carYear || val.year || 2022,
      color: val.vehicle?.color || val.carColor || val.color || 'White',
      licensePlate: val.vehicleNumber || val.licensePlate || val.plateNumber || val.vehicle?.licensePlate || val.plate || 'DRG-8B15',
      type: val.vehicle?.type || val.vehicleType || val.carType || 'sedan',
      seatingCapacity: val.vehicle?.seatingCapacity || 4,
      photoUrl: val.vehicleFrontUri || val.vehicle?.photoUrl || 'https://images.unsplash.com/photo-1549399542-7e3f8b79c341?w=400&auto=format&fit=crop&q=80',
      inspectionPassed: val.vehicle?.inspectionPassed !== false
    };

    const documents: DocumentVerification[] = parsedDocs.length > 0 ? parsedDocs : [
      {
        id: 'license',
        type: 'driver_license',
        title: "Driver's License",
        fileUrl: val.licenseImageUrl || val.licenseUrl || 'https://images.unsplash.com/photo-1557804506-669a67965ba0?w=600&auto=format&fit=crop&q=80',
        status: (val.licenseStatus || 'verified') as DocumentVerification['status'],
        documentNumber: val.licenseNumber || 'DL-8B15C-992',
        issueDate: '2023-01-15',
        expiryDate: '2028-01-15'
      },
      {
        id: 'vehicle_reg',
        type: 'vehicle_registration',
        title: 'Vehicle Registration',
        fileUrl: val.regImageUrl || 'https://images.unsplash.com/photo-1554224155-8d04cb21cd6c?w=600&auto=format&fit=crop&q=80',
        status: 'verified',
        documentNumber: 'VR-2026-X81',
        issueDate: '2023-02-10',
        expiryDate: '2027-02-10'
      }
    ];

    const telemetry: DeviceTelemetry = {
      deviceModel: val.telemetry?.deviceModel || val.deviceModel || 'Android Smartphone',
      manufacturer: val.telemetry?.manufacturer || 'Samsung',
      androidVersion: val.telemetry?.androidVersion || val.androidVersion || 'Android 13 (API 33)',
      apiLevel: val.telemetry?.apiLevel || 33,
      ramTotalGb: val.telemetry?.ramTotalGb || val.ramTotalGb || 4,
      ramUsagePercent: val.telemetry?.ramUsagePercent || 45,
      batteryLevel: val.telemetry?.batteryLevel || 88,
      isBatterySaver: val.telemetry?.isBatterySaver || false,
      appVersion: val.telemetry?.appVersion || 'v2.4.1',
      networkType: val.telemetry?.networkType || '4G',
      networkLatencyMs: val.telemetry?.networkLatencyMs || 42,
      gpsAccuracyMeters: val.telemetry?.gpsAccuracyMeters || 3.5,
      offlineQueuedPackets: val.telemetry?.offlineQueuedPackets || 0,
      lastPingAt: val.telemetry?.lastPingAt || 'Just now'
    };

    const driverObj: Driver = {
      id: key,
      fullName,
      phone,
      email,
      avatar,
      rating,
      totalTrips,
      acceptanceRate: typeof val.acceptanceRate === 'number' ? val.acceptanceRate : 96,
      completionRate: typeof val.completionRate === 'number' ? val.completionRate : 98,
      cancellationRate: typeof val.cancellationRate === 'number' ? val.cancellationRate : 2,
      status: status,
      accountStatus: driverAccountStatusVal,
      verificationStatus: verificationStatusVal,
      rawStatus: driverAccountStatusVal === 'SUSPENDED' ? 'SUSPENDED' : verificationStatusVal,
      walletBalance,
      todayEarnings: typeof val.todayEarnings === 'number' ? val.todayEarnings : 48.5,
      joinedDate: val.joinedDate || '2024-03-15',
      currentLocation: {
        lat: val.currentLocation?.lat || val.currentLocation?.latitude || 33.6844,
        lng: val.currentLocation?.lng || val.currentLocation?.longitude || 73.0479,
        heading: val.currentLocation?.heading || 180,
        speedKmh: val.currentLocation?.speedKmh || 25
      },
      vehicle,
      documents,
      telemetry,
      city: val.city || 'Islamabad'
    };

    return { driver: driverObj, rawRole: 'driver' };
  } else {
    const riderObj: Rider = {
      id: key,
      fullName,
      phone,
      email,
      avatar,
      rating,
      totalRides: totalTrips,
      totalSpend: typeof val.totalSpend === 'number' ? val.totalSpend : 85.0,
      walletBalance,
      status: (passengerAccountStatusVal === 'FLAGGED' ? 'flagged' : passengerAccountStatusVal === 'SUSPENDED' ? 'suspended' : passengerAccountStatusVal === 'DEACTIVATED' ? 'deactivated' : passengerAccountStatusVal === 'INACTIVE' ? 'inactive' : 'active') as RiderStatus,
      accountStatus: passengerAccountStatusVal,
      joinedDate: val.joinedDate || '2024-05-10',
      preferredPayment: (val.preferredPayment || val.paymentMethod || 'cash') as PaymentMethod,
      deviceModel: val.deviceModel || 'Redmi Note 12',
      androidVersion: val.androidVersion || 'Android 12',
      emergencyContact: val.emergencyContact || { name: 'Emergency Contact', phone: phone, relationship: 'Family' },
      reportedIncidentsCount: typeof val.reportedIncidentsCount === 'number' ? val.reportedIncidentsCount : 0,
      city: val.city || 'Islamabad'
    };

    return { rider: riderObj, rawRole: 'rider' };
  }
}

/**
 * Real-time listener for ALL users from /users node (with persistent live sync to /drivers, /riders, and /wallets across RTDB & Firestore)
 */
export function subscribeToAllUsers(
  callback: (data: { drivers: Driver[]; riders: Rider[] }) => void
): () => void {
  const { db, firestore } = initFirebaseService();
  const unsubs: Array<() => void> = [];

  const rtdbUsers: Record<string, any> = {};
  const rtdbDrivers: Record<string, any> = {};
  const rtdbRiders: Record<string, any> = {};
  const rtdbWallets: Record<string, any> = {};
  const rtdbStats: Record<string, any> = {};
  const rtdbTrips: Record<string, any> = {};

  const firestoreUsers: Record<string, any> = {};
  const firestoreDrivers: Record<string, any> = {};
  const firestoreRiders: Record<string, any> = {};
  const firestoreWallets: Record<string, any> = {};
  const firestoreStats: Record<string, any> = {};
  const firestoreTrips: Record<string, any> = {};

  const emitAggregated = () => {
    // Collect all unique user IDs across all database nodes
    const allUserIds = new Set<string>([
      ...Object.keys(rtdbUsers),
      ...Object.keys(rtdbDrivers),
      ...Object.keys(rtdbRiders),
      ...Object.keys(rtdbWallets),
      ...Object.keys(rtdbStats),
      ...Object.keys(firestoreUsers),
      ...Object.keys(firestoreDrivers),
      ...Object.keys(firestoreRiders),
      ...Object.keys(firestoreWallets),
      ...Object.keys(firestoreStats),
    ]);

    const allTripsList = [
      ...Object.values(rtdbTrips),
      ...Object.values(firestoreTrips),
    ];

    const driversMap: Record<string, Driver> = {};
    const ridersMap: Record<string, Rider> = {};

    allUserIds.forEach((id) => {
      // Merge all records for this user ID in priority order
      const mergedVal: any = {
        ...(rtdbUsers[id] || {}),
        ...(rtdbDrivers[id] || {}),
        ...(rtdbRiders[id] || {}),
        ...(rtdbStats[id] || {}),
        ...(firestoreUsers[id] || {}),
        ...(firestoreDrivers[id] || {}),
        ...(firestoreRiders[id] || {}),
        ...(firestoreStats[id] || {}),
      };

      // Determine wallet balance explicitly if present in wallets node or merged data
      const explicitWalletVal = rtdbWallets[id] !== undefined ? rtdbWallets[id] : firestoreWallets[id];
      let balance = 0;
      if (explicitWalletVal !== undefined && explicitWalletVal !== null) {
        balance = extractWalletBalance(explicitWalletVal);
      } else {
        balance = extractWalletBalance(mergedVal);
      }
      mergedVal.walletBalance = balance;
      mergedVal.balance = balance;

      // Extract explicit total trips/rides from object
      const extractedTrips = extractTotalTrips(mergedVal);

      // Compute completed trips from live trip collections if matching driver or rider
      const liveDriverCompletedCount = allTripsList.filter((t: any) => {
        if (!t || typeof t !== 'object') return false;
        const isMatch = t.driverId === id || t.driver_id === id || t.driver?.id === id;
        const statusLower = String(t.status || '').toLowerCase();
        const isCompleted = statusLower === 'completed' || statusLower === 'finished' || statusLower === 'ended' || t.isCompleted === true;
        return isMatch && isCompleted;
      }).length;

      const liveRiderCompletedCount = allTripsList.filter((t: any) => {
        if (!t || typeof t !== 'object') return false;
        const isMatch = t.riderId === id || t.rider_id === id || t.passengerId === id || t.passenger_id === id || t.rider?.id === id;
        const statusLower = String(t.status || '').toLowerCase();
        const isCompleted = statusLower === 'completed' || statusLower === 'finished' || statusLower === 'ended' || t.isCompleted === true;
        return isMatch && isCompleted;
      }).length;

      if (Object.keys(mergedVal).length > 0) {
        const normalized = normalizeFirebaseUser(id, mergedVal);
        if (normalized.driver) {
          normalized.driver.walletBalance = balance;
          normalized.driver.totalTrips = Math.max(extractedTrips, liveDriverCompletedCount, normalized.driver.totalTrips || 0);
          driversMap[id] = normalized.driver;
        }
        if (normalized.rider) {
          normalized.rider.walletBalance = balance;
          normalized.rider.totalRides = Math.max(extractedTrips, liveRiderCompletedCount, normalized.rider.totalRides || 0);
          ridersMap[id] = normalized.rider;
        }
      }
    });

    callback({
      drivers: Object.values(driversMap),
      riders: Object.values(ridersMap)
    });
  };

  // 1. RTDB Real-time Listeners
  if (db) {
    try {
      const usersRef = ref(db, 'users');
      const u1 = onValue(
        usersRef,
        (snapshot) => {
          const val = snapshot.val() || {};
          Object.keys(rtdbUsers).forEach((k) => delete rtdbUsers[k]);
          if (val && typeof val === 'object') Object.assign(rtdbUsers, val);
          emitAggregated();
        },
        (err) => console.warn('RTDB users note:', err)
      );
      unsubs.push(u1);

      const driversRef = ref(db, 'drivers');
      const u2 = onValue(
        driversRef,
        (snapshot) => {
          const val = snapshot.val() || {};
          Object.keys(rtdbDrivers).forEach((k) => delete rtdbDrivers[k]);
          if (val && typeof val === 'object') Object.assign(rtdbDrivers, val);
          emitAggregated();
        },
        (err) => console.warn('RTDB drivers note:', err)
      );
      unsubs.push(u2);

      const ridersRef = ref(db, 'riders');
      const u3 = onValue(
        ridersRef,
        (snapshot) => {
          const val = snapshot.val() || {};
          Object.keys(rtdbRiders).forEach((k) => delete rtdbRiders[k]);
          if (val && typeof val === 'object') Object.assign(rtdbRiders, val);
          emitAggregated();
        },
        (err) => console.warn('RTDB riders note:', err)
      );
      unsubs.push(u3);

      const walletsRef = ref(db, 'wallets');
      const u4 = onValue(
        walletsRef,
        (snapshot) => {
          const val = snapshot.val() || {};
          Object.keys(rtdbWallets).forEach((k) => delete rtdbWallets[k]);
          if (val && typeof val === 'object') Object.assign(rtdbWallets, val);
          emitAggregated();
        },
        (err) => console.warn('RTDB wallets note:', err)
      );
      unsubs.push(u4);

      const statsRef = ref(db, 'driver_stats');
      const u5 = onValue(
        statsRef,
        (snapshot) => {
          const val = snapshot.val() || {};
          Object.keys(rtdbStats).forEach((k) => delete rtdbStats[k]);
          if (val && typeof val === 'object') Object.assign(rtdbStats, val);
          emitAggregated();
        },
        (err) => console.warn('RTDB stats note:', err)
      );
      unsubs.push(u5);

      const tripsRef = ref(db, 'trips');
      const u6 = onValue(
        tripsRef,
        (snapshot) => {
          const val = snapshot.val() || {};
          Object.keys(rtdbTrips).forEach((k) => delete rtdbTrips[k]);
          if (val && typeof val === 'object') Object.assign(rtdbTrips, val);
          emitAggregated();
        },
        (err) => console.warn('RTDB trips note:', err)
      );
      unsubs.push(u6);
    } catch (e) {
      console.warn('Error setting up RTDB user listeners:', e);
    }
  }

  // 2. Firestore Real-time Listeners
  if (firestore) {
    try {
      const colUsers = collection(firestore, 'users');
      const uF1 = onSnapshot(
        colUsers,
        (snapshot) => {
          Object.keys(firestoreUsers).forEach((k) => delete firestoreUsers[k]);
          snapshot.forEach((docSnap) => {
            firestoreUsers[docSnap.id] = docSnap.data();
          });
          emitAggregated();
        },
        (err) => console.warn('Firestore users note:', err)
      );
      unsubs.push(uF1);

      const colDrivers = collection(firestore, 'drivers');
      const uF2 = onSnapshot(
        colDrivers,
        (snapshot) => {
          Object.keys(firestoreDrivers).forEach((k) => delete firestoreDrivers[k]);
          snapshot.forEach((docSnap) => {
            firestoreDrivers[docSnap.id] = docSnap.data();
          });
          emitAggregated();
        },
        (err) => console.warn('Firestore drivers note:', err)
      );
      unsubs.push(uF2);

      const colRiders = collection(firestore, 'riders');
      const uF3 = onSnapshot(
        colRiders,
        (snapshot) => {
          Object.keys(firestoreRiders).forEach((k) => delete firestoreRiders[k]);
          snapshot.forEach((docSnap) => {
            firestoreRiders[docSnap.id] = docSnap.data();
          });
          emitAggregated();
        },
        (err) => console.warn('Firestore riders note:', err)
      );
      unsubs.push(uF3);

      const colWallets = collection(firestore, 'wallets');
      const uF4 = onSnapshot(
        colWallets,
        (snapshot) => {
          Object.keys(firestoreWallets).forEach((k) => delete firestoreWallets[k]);
          snapshot.forEach((docSnap) => {
            firestoreWallets[docSnap.id] = docSnap.data();
          });
          emitAggregated();
        },
        (err) => console.warn('Firestore wallets note:', err)
      );
      unsubs.push(uF4);

      const colStats = collection(firestore, 'driver_stats');
      const uF5 = onSnapshot(
        colStats,
        (snapshot) => {
          Object.keys(firestoreStats).forEach((k) => delete firestoreStats[k]);
          snapshot.forEach((docSnap) => {
            firestoreStats[docSnap.id] = docSnap.data();
          });
          emitAggregated();
        },
        (err) => console.warn('Firestore stats note:', err)
      );
      unsubs.push(uF5);

      const colTrips = collection(firestore, 'trips');
      const uF6 = onSnapshot(
        colTrips,
        (snapshot) => {
          Object.keys(firestoreTrips).forEach((k) => delete firestoreTrips[k]);
          snapshot.forEach((docSnap) => {
            firestoreTrips[docSnap.id] = docSnap.data();
          });
          emitAggregated();
        },
        (err) => console.warn('Firestore trips note:', err)
      );
      unsubs.push(uF6);
    } catch (e) {
      console.warn('Error setting up Firestore user listeners:', e);
    }
  }

  return () => {
    unsubs.forEach((u) => {
      try { u(); } catch { /* ignore */ }
    });
  };
}

/**
 * Real-time listener for Drivers
 */
export function subscribeToDrivers(callback: (drivers: Driver[]) => void): () => void {
  return subscribeToAllUsers(({ drivers }) => callback(drivers));
}

/**
 * Real-time listener for live_driver_locations and driver_locations nodes in Firebase RTDB / Firestore
 */
export function subscribeToLiveDriverLocations(
  callback: (locations: Record<string, LiveDriverLocation>) => void
): () => void {
  const { db, firestore } = initFirebaseService();

  if (db) {
    const locRef1 = ref(db, 'live_driver_locations');
    const locRef2 = ref(db, 'driver_locations');
    const combinedMap: Record<string, LiveDriverLocation> = {};

    const processSnapshot = (snapshot: any) => {
      const val = snapshot.val();
      if (val && typeof val === 'object') {
        Object.entries(val).forEach(([key, record]: [string, any]) => {
          if (record && typeof record === 'object') {
            const lat =
              typeof record.lat === 'number'
                ? record.lat
                : typeof record.latitude === 'number'
                ? record.latitude
                : null;
            const lng =
              typeof record.lng === 'number'
                ? record.lng
                : typeof record.longitude === 'number'
                ? record.longitude
                : null;
            if (lat !== null && lng !== null) {
              const dId = record.driverId || record.id || key;
              combinedMap[dId] = {
                driverId: dId,
                driverName: record.driverName || record.name || record.fullName || 'Driver',
                phone: record.phone || record.phoneNumber || '',
                lat,
                lng,
                heading: typeof record.heading === 'number' ? record.heading : typeof record.bearing === 'number' ? record.bearing : 0,
                speedKmh: typeof record.speedKmh === 'number' ? record.speedKmh : typeof record.speed === 'number' ? record.speed : 0,
                updatedAt: record.updatedAt || record.timestamp || record.lastUpdated || 'Just now',
                status: record.status || record.driverStatus || 'online',
                vehicleType: record.vehicleType || record.type || record.vehicle?.type || 'sedan',
                licensePlate: record.licensePlate || record.plate || record.vehicle?.licensePlate || ''
              };
            }
          }
        });
      }
      callback({ ...combinedMap });
    };

    const unsub1 = onValue(
      locRef1,
      (snap) => processSnapshot(snap),
      (err) => console.warn('RTDB live_driver_locations listener note:', err)
    );
    const unsub2 = onValue(
      locRef2,
      (snap) => processSnapshot(snap),
      (err) => console.warn('RTDB driver_locations listener note:', err)
    );

    return () => {
      unsub1();
      unsub2();
    };
  }

  if (firestore) {
    const colRef1 = collection(firestore, 'live_driver_locations');
    const colRef2 = collection(firestore, 'driver_locations');
    const combinedMap: Record<string, LiveDriverLocation> = {};

    let snap1Data = new Map<string, any>();
    let snap2Data = new Map<string, any>();

    const updateCombined = () => {
      const map: Record<string, LiveDriverLocation> = {};
      const processDocs = (snapshot: any) => {
        snapshot.forEach((docSnap: any) => {
          const data = docSnap.data();
          const lat =
            typeof data.lat === 'number'
              ? data.lat
              : typeof data.latitude === 'number'
              ? data.latitude
              : null;
          const lng =
            typeof data.lng === 'number'
              ? data.lng
              : typeof data.longitude === 'number'
              ? data.longitude
              : null;
          if (lat !== null && lng !== null) {
            const dId = data.driverId || data.id || docSnap.id;
            map[dId] = {
              driverId: dId,
              driverName: data.driverName || data.name || data.fullName || 'Driver',
              phone: data.phone || data.phoneNumber || '',
              lat,
              lng,
              heading: typeof data.heading === 'number' ? data.heading : typeof data.bearing === 'number' ? data.bearing : 0,
              speedKmh: typeof data.speedKmh === 'number' ? data.speedKmh : typeof data.speed === 'number' ? data.speed : 0,
              updatedAt: data.updatedAt || data.timestamp || data.lastUpdated || 'Just now',
              status: data.status || data.driverStatus || 'online',
              vehicleType: data.vehicleType || data.type || data.vehicle?.type || 'sedan',
              licensePlate: data.licensePlate || data.plate || data.vehicle?.licensePlate || ''
            };
          }
        });
      };
      processDocs(snap1Data);
      processDocs(snap2Data);
      callback(map);
    };

    const unsub1 = onSnapshot(
      colRef1,
      (snapshot) => {
        snap1Data = snapshot as any;
        updateCombined();
      },
      (err) => console.warn('Firestore live_driver_locations listener note:', err)
    );

    const unsub2 = onSnapshot(
      colRef2,
      (snapshot) => {
        snap2Data = snapshot as any;
        updateCombined();
      },
      (err) => console.warn('Firestore driver_locations listener note:', err)
    );

    return () => {
      unsub1();
      unsub2();
    };
  }

  callback({});
  return () => {};
}

/**
 * Real-time listener for driver_verifications collection
 */
export function subscribeToDriverVerifications(
  callback: (verifications: DriverVerification[]) => void
): () => void {
  const { db, firestore } = initFirebaseService();

  if (db) {
    const verifRef = ref(db, 'driver_verifications');
    const unsub = onValue(verifRef, (snapshot) => {
      const val = snapshot.val();
      const list: DriverVerification[] = [];
      if (val && typeof val === 'object') {
        Object.entries(val).forEach(([key, record]: [string, any]) => {
          if (record && typeof record === 'object') {
            const docs = extractDriverDocuments(record);
            
            const recordAccountStatus = String(record.accountStatus || '').toUpperCase();
            const isSuspended = recordAccountStatus === 'SUSPENDED' || String(record.status || '').toLowerCase() === 'suspended';
            const accountStatusVal: 'ACTIVE' | 'SUSPENDED' = isSuspended ? 'SUSPENDED' : 'ACTIVE';
            
            let verifStatusVal = 'APPROVED';
            if (record.verificationStatus) {
              verifStatusVal = String(record.verificationStatus).toUpperCase();
            } else if (record.status && !['SUSPENDED', 'suspended', 'ACTIVE', 'active'].includes(record.status)) {
              verifStatusVal = String(record.status).toUpperCase();
            } else if (docs.some(d => d.status === 'pending')) {
              verifStatusVal = 'PENDING';
            }

            list.push({
              id: key,
              driverId: record.driverId || record.id || key,
              driverName: record.driverName || record.fullName || record.name || 'Driver',
              fullName: record.fullName || record.driverName || record.name || 'Driver',
              profileImage: record.profileImage || record.avatar || record.photoUrl || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80',
              avatar: record.avatar || record.profileImage || record.photoUrl || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80',
              documentId: record.documentId || record.docId || key,
              role: 'Driver',
              phone: record.phone || record.phoneNumber || 'N/A',
              email: record.email || `${key.slice(0, 8)}@drigo.app`,
              vehicleMake: record.vehicleMake || record.vehicleCompany || record.make || record.vehicle?.make || 'Toyota',
              vehicleModel: record.vehicleModel || record.model || record.vehicle?.model || 'Corolla',
              licensePlate: record.licensePlate || record.vehicleNumber || record.plate || record.vehicle?.licensePlate || 'DRG-8B15',
              status: isSuspended ? 'SUSPENDED' : verifStatusVal,
              accountStatus: accountStatusVal,
              verificationStatus: verifStatusVal,
              submittedAt: record.submittedAt || record.createdAt || 'Just now',
              documents: docs
            });
          }
        });
      }
      callback(list);
    }, (err) => {
      console.warn('RTDB driver_verifications listener note:', err);
    });
    return () => unsub();
  }

  if (firestore) {
    const colRef = collection(firestore, 'driver_verifications');
    const unsub = onSnapshot(colRef, (snapshot) => {
      const list: DriverVerification[] = [];
      snapshot.forEach(docSnap => {
        const data = docSnap.data();
        const isSuspended = String(data.accountStatus || '').toUpperCase() === 'SUSPENDED' || String(data.status || '').toLowerCase() === 'suspended';
        const accountStatusVal: 'ACTIVE' | 'SUSPENDED' = isSuspended ? 'SUSPENDED' : 'ACTIVE';
        const verifStatusVal = String(data.verificationStatus || data.status || 'APPROVED').toUpperCase();
        
        list.push({
          id: docSnap.id,
          driverId: data.driverId || docSnap.id,
          driverName: data.driverName || data.fullName || data.name || 'Driver',
          fullName: data.fullName || data.driverName || data.name || 'Driver',
          profileImage: data.profileImage || data.avatar || data.photoUrl || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80',
          avatar: data.avatar || data.profileImage || data.photoUrl || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80',
          documentId: data.documentId || data.docId || docSnap.id,
          role: 'Driver',
          phone: data.phone || data.phoneNumber || 'N/A',
          email: data.email || `${docSnap.id.slice(0, 8)}@drigo.app`,
          vehicleMake: data.vehicleMake || data.make || data.vehicle?.make || 'Toyota',
          vehicleModel: data.vehicleModel || data.model || data.vehicle?.model || 'Corolla',
          licensePlate: data.licensePlate || data.plate || data.vehicle?.licensePlate || 'DRG-8B15',
          status: isSuspended ? 'SUSPENDED' : verifStatusVal,
          accountStatus: accountStatusVal,
          verificationStatus: verifStatusVal,
          submittedAt: data.submittedAt || data.createdAt || 'Just now',
        });
      });
      callback(list);
    }, (err) => {
      console.warn('Firestore driver_verifications listener note:', err);
    });
    return () => unsub();
  }

  callback([]);
  return () => {};
}

/**
 * Real-time listener for Passengers / Riders
 */
export function subscribeToRiders(callback: (riders: Rider[]) => void): () => void {
  return subscribeToAllUsers(({ riders }) => callback(riders));
}

/**
 * Real-time listener for Trips / Rides (RTDB & Firestore fallback)
 */
export function normalizeFirebaseTrip(key: string, r: any): Trip {
  const fareVal = Number(
    r.agreedFare ||
    r.assignedFare ||
    r.estimatedFare ||
    (typeof r.fare === 'number' ? r.fare : r.fare?.total) ||
    r.amount ||
    r.offerAmount ||
    r.price ||
    450
  );

  let rawFromLat = (r.fromLocation?.lat !== undefined && !isNaN(Number(r.fromLocation.lat)))
    ? r.fromLocation.lat
    : (r.pickupLat !== undefined ? r.pickupLat : (r.pickupLatitude !== undefined ? r.pickupLatitude : (r.lat !== undefined ? r.lat : (r.pickup?.lat || 34.0151))));
  let rawFromLng = (r.fromLocation?.lng !== undefined && !isNaN(Number(r.fromLocation.lng)))
    ? r.fromLocation.lng
    : (r.pickupLon !== undefined ? r.pickupLon : (r.pickupLongitude !== undefined ? r.pickupLongitude : (r.lng !== undefined ? r.lng : (r.pickup?.lng || 71.5249))));
  let rawToLat = (r.toLocation?.lat !== undefined && !isNaN(Number(r.toLocation.lat)))
    ? r.toLocation.lat
    : (r.destinationLat !== undefined ? r.destinationLat : (r.destinationLatitude !== undefined ? r.destinationLatitude : (r.dropoffLat !== undefined ? r.dropoffLat : (r.destination?.lat || 34.0044))));
  let rawToLng = (r.toLocation?.lng !== undefined && !isNaN(Number(r.toLocation.lng)))
    ? r.toLocation.lng
    : (r.destinationLon !== undefined ? r.destinationLon : (r.destinationLongitude !== undefined ? r.destinationLongitude : (r.dropoffLon !== undefined ? r.dropoffLon : (r.destination?.lng || 71.5369))));

  const pickupTitleStr = `${r.pickupTitle || r.fromLocation?.name || r.pickup || ''} ${r.pickupSubtitle || r.fromLocation?.address || ''}`.toLowerCase();
  const destTitleStr = `${r.destinationTitle || r.toLocation?.name || r.destination || ''} ${r.destinationSubtitle || r.toLocation?.address || ''}`.toLowerCase();

  // Smart Peshawar landmark coordinates if not accurately set by GPS
  if (pickupTitleStr.includes('shero jahngi') || pickupTitleStr.includes('street number 9')) {
    rawFromLat = 34.0205;
    rawFromLng = 71.5750;
  } else if (pickupTitleStr.includes('hayatabad') && pickupTitleStr.includes('phase 3')) {
    rawFromLat = 33.9925;
    rawFromLng = 71.4380;
  }

  if (destTitleStr.includes('hayatabad') && (destTitleStr.includes('phase 3') || destTitleStr.includes('tatara park'))) {
    rawToLat = 33.9925;
    rawToLng = 71.4380;
  } else if (destTitleStr.includes('saddar')) {
    rawToLat = 34.0044;
    rawToLng = 71.5369;
  }

  const fromLat = Number(rawFromLat);
  const fromLng = Number(rawFromLng);
  const toLat = Number(rawToLat);
  const toLng = Number(rawToLng);

  // Normalize driver bids / offers from inDrive-style Android bidding
  const rawBids = r.bids || r.offers || r.driverOffers || [];
  let parsedBids: any[] = [];
  if (Array.isArray(rawBids)) {
    parsedBids = rawBids.map((b: any, idx: number) => ({
      driverId: b.driverId || b.id || `bid-${idx}`,
      driverName: b.driverName || b.name || 'Driver',
      driverPhone: b.driverPhone || b.phone || '',
      driverAvatar: b.driverAvatar || b.photoUrl || 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=100',
      driverRating: Number(b.driverRating || b.rating || 4.9),
      offerAmount: Number(b.offerAmount || b.fare || b.amount || b.price || fareVal),
      vehicleModel: b.vehicleModel || b.vehicle || '',
      vehiclePlate: b.vehiclePlate || b.plateNumber || '',
      etaMinutes: Number(b.etaMinutes || b.eta || b.arrivalMinutes || 5),
      createdAt: b.createdAt || b.timestamp || 'Just now',
      status: (b.status || 'pending') as 'pending' | 'accepted' | 'rejected',
    }));
  } else if (rawBids && typeof rawBids === 'object') {
    parsedBids = Object.entries(rawBids).map(([bId, b]: [string, any]) => ({
      driverId: b.driverId || bId,
      driverName: b.driverName || b.name || 'Driver',
      driverPhone: b.driverPhone || b.phone || '',
      driverAvatar: b.driverAvatar || b.photoUrl || 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=100',
      driverRating: Number(b.driverRating || b.rating || 4.9),
      offerAmount: Number(b.offerAmount || b.fare || b.amount || b.price || fareVal),
      vehicleModel: b.vehicleModel || b.vehicle || '',
      vehiclePlate: b.vehiclePlate || b.plateNumber || '',
      etaMinutes: Number(b.etaMinutes || b.eta || b.arrivalMinutes || 5),
      createdAt: b.createdAt || b.timestamp || 'Just now',
      status: (b.status || 'pending') as 'pending' | 'accepted' | 'rejected',
    }));
  }

  // Normalize status
  const rawStatus = String(r.status || 'requested').toLowerCase();
  let normalizedStatus: TripStatus = 'requested';
  if (['requested', 'searching', 'matching', 'finding_driver', 'pending'].includes(rawStatus)) {
    normalizedStatus = parsedBids.length > 0 ? 'offer_received' : 'requested';
  } else if (['offer_received', 'bidding', 'offers_received', 'has_bids'].includes(rawStatus)) {
    normalizedStatus = 'offer_received';
  } else if (['accepted', 'driver_matched', 'confirmed'].includes(rawStatus)) {
    normalizedStatus = 'accepted';
  } else if (['driver_arriving', 'arriving', 'on_the_way', 'coming'].includes(rawStatus)) {
    normalizedStatus = 'driver_arriving';
  } else if (['driver_arrived', 'arrived'].includes(rawStatus)) {
    normalizedStatus = 'driver_arrived';
  } else if (['in_progress', 'in_trip', 'started', 'on_trip', 'active'].includes(rawStatus)) {
    normalizedStatus = 'in_progress';
  } else if (['completed', 'finished', 'done', 'paid'].includes(rawStatus)) {
    normalizedStatus = 'completed';
  } else if (['cancelled', 'canceled', 'rejected', 'expired'].includes(rawStatus)) {
    normalizedStatus = 'cancelled';
  } else if (['sos_alert', 'sos', 'emergency'].includes(rawStatus)) {
    normalizedStatus = 'sos_alert';
  }

  // Format request timestamp
  let reqAt = r.createdAt || r.requestedAt || r.timestamp || 'Just now';
  if (typeof reqAt === 'number') {
    try {
      reqAt = new Date(reqAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
    } catch {
      reqAt = 'Just now';
    }
  }

  return {
    id: key,
    tripCode: r.tripCode || (key.length <= 8 ? `DRG-${key.toUpperCase()}` : `DRG-${key.slice(0, 5).toUpperCase()}`),
    passengerId: r.passengerId || r.riderId || r.userId || 'rider-1',
    passengerName: r.passengerName || r.riderName || r.userName || 'Passenger',
    passengerPhone: r.passengerPhone || r.userPhone || r.phone || 'N/A',
    passengerAvatar: r.passengerPhotoUrl || r.passengerAvatar || r.photoUrl || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100',
    passengerRating: Number(r.passengerRating || r.riderRating || 4.9),
    driverId: r.assignedDriverId || r.driverId || undefined,
    driverName: r.assignedDriverName || r.driverName || undefined,
    driverPhone: r.driverPhone || r.assignedDriverPhone || '',
    driverAvatar: r.driverAvatar || r.driverPhotoUrl || (r.driverName ? 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=100' : undefined),
    driverRating: Number(r.driverRating || 5.0),
    vehiclePlate: r.driverPlateNumber || r.vehiclePlate || r.vehicleNumber || '',
    vehicleModel: r.driverVehicleModel ? `${r.driverVehicleMake || ''} ${r.driverVehicleModel}` : (r.vehicleModel || r.vehicleType || 'Car'),
    vehicleColor: r.vehicleColor || r.color || '',
    ...(() => {
      const resolvedFrom = resolvePhysicalAddress({
        name: r.pickupTitle || r.fromLocation?.name || r.pickupAddress || r.pickup || 'Pickup Location',
        address: r.pickupSubtitle || r.fromLocation?.address || r.pickup || 'Peshawar, Khyber Pakhtunkhwa',
        lat: isNaN(fromLat) ? 34.0151 : fromLat,
        lng: isNaN(fromLng) ? 71.5249 : fromLng
      });
      const resolvedTo = resolvePhysicalAddress({
        name: r.destinationTitle || r.toLocation?.name || r.destinationAddress || r.destination || 'Destination',
        address: r.destinationSubtitle || r.toLocation?.address || r.destination || 'Peshawar, Khyber Pakhtunkhwa',
        lat: isNaN(toLat) ? 34.0044 : toLat,
        lng: isNaN(toLng) ? 71.5369 : toLng
      });

      const accurateRoute = calculateAccurateRouteDistance(
        resolvedFrom.lat,
        resolvedFrom.lng,
        resolvedTo.lat,
        resolvedTo.lng,
        Number(r.distanceKm || r.distance),
        Number(r.durationMinutes || r.estimatedDurationMinutes)
      );

      return {
        fromLocation: {
          name: resolvedFrom.name,
          address: resolvedFrom.physicalAddress,
          lat: resolvedFrom.lat,
          lng: resolvedFrom.lng
        },
        toLocation: {
          name: resolvedTo.name,
          address: resolvedTo.physicalAddress,
          lat: resolvedTo.lat,
          lng: resolvedTo.lng
        },
        distanceKm: accurateRoute.distanceKm,
        estimatedDurationMinutes: accurateRoute.durationMinutes,
      };
    })(),
    actualDurationMinutes: r.actualDurationMinutes ? Number(r.actualDurationMinutes) : undefined,
    routePolyline: r.routePolyline || [],
    fare: typeof r.fare === 'object' && r.fare?.total ? r.fare : {
      baseFare: 50,
      distanceFare: Math.max(0, fareVal - 50),
      timeFare: 0,
      surgeMultiplier: 1.0,
      surgeAmount: 0,
      discount: 0,
      drigoCommissionRate: 0.15,
      drigoCommissionAmount: Math.round(fareVal * 0.15),
      driverEarnings: Math.round(fareVal * 0.85),
      total: fareVal,
      currency: 'PKR'
    },
    offerAmount: r.offerAmount ? Number(r.offerAmount) : fareVal,
    counterOfferAmount: r.counterOfferAmount ? Number(r.counterOfferAmount) : undefined,
    bids: parsedBids,
    rideCategory: r.rideCategory || r.vehicleType || 'Standard',
    notes: r.notes || r.comment || '',
    vehicleType: (r.vehicleType || r.serviceType || 'sedan') as VehicleType,
    status: normalizedStatus,
    requestedAt: reqAt,
    startedAt: r.startedAt || undefined,
    completedAt: r.completedAt || undefined,
    cancelledAt: r.cancelledAt || undefined,
    cancellationReason: r.cancellationReason || r.cancelReason || undefined,
    cancelledBy: r.cancelledBy || undefined,
    paymentMethod: (r.paymentMethod || 'cash') as PaymentMethod,
    paymentStatus: (r.paymentStatus || 'paid') as PaymentStatus,
    sosAlert: r.sosAlert || (normalizedStatus === 'sos_alert' ? { isTriggered: true, reason: 'SOS Alert raised' } : undefined),
    rating: r.rating ? Number(r.rating) : undefined,
    review: r.review || undefined
  };
}

/**
 * Real-time listener for Trips / Ride Requests (combining Realtime Database & Firestore)
 * Directly uses real data synchronized from the Android passenger & driver apps.
 * Never replaces or overrides real data with mock items.
 */
export function subscribeToTrips(callback: (trips: Trip[]) => void): () => void {
  const { db, firestore } = initFirebaseService();
  const unsubs: (() => void)[] = [];

  const rtdbTrips: Record<string, any> = {};
  const rtdbRequests: Record<string, any> = {};
  const rtdbOrders: Record<string, any> = {};
  const rtdbRides: Record<string, any> = {};

  const firestoreTrips: Record<string, any> = {};
  const firestoreRequests: Record<string, any> = {};
  const firestoreRides: Record<string, any> = {};

  const emitAggregated = () => {
    const combinedMap: Record<string, Trip> = {};

    // 1. Process RTDB trips
    Object.entries(rtdbTrips).forEach(([k, val]) => {
      if (val && typeof val === 'object') combinedMap[k] = normalizeFirebaseTrip(k, val);
    });

    // 2. Process RTDB ride_requests
    Object.entries(rtdbRequests).forEach(([k, val]) => {
      if (val && typeof val === 'object') {
        const parsed = normalizeFirebaseTrip(k, val);
        combinedMap[k] = combinedMap[k] ? { ...combinedMap[k], ...parsed } : parsed;
      }
    });

    // 3. Process RTDB rides
    Object.entries(rtdbRides).forEach(([k, val]) => {
      if (val && typeof val === 'object') {
        const parsed = normalizeFirebaseTrip(k, val);
        combinedMap[k] = combinedMap[k] ? { ...combinedMap[k], ...parsed } : parsed;
      }
    });

    // 4. Process RTDB passenger_orders overlay
    Object.entries(rtdbOrders).forEach(([k, val]) => {
      if (val && typeof val === 'object') {
        if (combinedMap[k]) {
          if (val.agreedFare || val.assignedFare) {
            combinedMap[k].fare.total = Number(val.agreedFare || val.assignedFare);
          }
          if (val.status) {
            combinedMap[k].status = (String(val.status).toLowerCase()) as TripStatus;
          }
        } else {
          combinedMap[k] = normalizeFirebaseTrip(k, val);
        }
      }
    });

    // 5. Process Firestore trips
    Object.entries(firestoreTrips).forEach(([k, val]) => {
      if (val && typeof val === 'object') {
        const parsed = normalizeFirebaseTrip(k, val);
        combinedMap[k] = combinedMap[k] ? { ...combinedMap[k], ...parsed } : parsed;
      }
    });

    // 6. Process Firestore ride_requests
    Object.entries(firestoreRequests).forEach(([k, val]) => {
      if (val && typeof val === 'object') {
        const parsed = normalizeFirebaseTrip(k, val);
        combinedMap[k] = combinedMap[k] ? { ...combinedMap[k], ...parsed } : parsed;
      }
    });

    // 7. Process Firestore rides
    Object.entries(firestoreRides).forEach(([k, val]) => {
      if (val && typeof val === 'object') {
        const parsed = normalizeFirebaseTrip(k, val);
        combinedMap[k] = combinedMap[k] ? { ...combinedMap[k], ...parsed } : parsed;
      }
    });

    const resultList = Object.values(combinedMap);
    callback(resultList);
  };

  // Attach Realtime Database Listeners
  if (db) {
    try {
      const tripsRef = ref(db, 'trips');
      const u1 = onValue(tripsRef, (snap) => {
        const val = snap.val() || {};
        Object.keys(rtdbTrips).forEach(k => delete rtdbTrips[k]);
        if (val && typeof val === 'object') Object.assign(rtdbTrips, val);
        emitAggregated();
      }, (err) => console.warn('RTDB trips note:', err));
      unsubs.push(u1);

      const requestsRef = ref(db, 'ride_requests');
      const u2 = onValue(requestsRef, (snap) => {
        const val = snap.val() || {};
        Object.keys(rtdbRequests).forEach(k => delete rtdbRequests[k]);
        if (val && typeof val === 'object') Object.assign(rtdbRequests, val);
        emitAggregated();
      }, (err) => console.warn('RTDB ride_requests note:', err));
      unsubs.push(u2);

      const ridesRef = ref(db, 'rides');
      const u3 = onValue(ridesRef, (snap) => {
        const val = snap.val() || {};
        Object.keys(rtdbRides).forEach(k => delete rtdbRides[k]);
        if (val && typeof val === 'object') Object.assign(rtdbRides, val);
        emitAggregated();
      }, (err) => console.warn('RTDB rides note:', err));
      unsubs.push(u3);

      const ordersRef = ref(db, 'passenger_orders');
      const u4 = onValue(ordersRef, (snap) => {
        const val = snap.val() || {};
        Object.keys(rtdbOrders).forEach(k => delete rtdbOrders[k]);
        if (val && typeof val === 'object') Object.assign(rtdbOrders, val);
        emitAggregated();
      }, (err) => console.warn('RTDB passenger_orders note:', err));
      unsubs.push(u4);
    } catch (e) {
      console.warn('Error setting up RTDB trip listeners:', e);
    }
  }

  // Attach Firestore Listeners
  if (firestore) {
    try {
      const colTrips = collection(firestore, 'trips');
      const uF1 = onSnapshot(colTrips, (snap) => {
        Object.keys(firestoreTrips).forEach(k => delete firestoreTrips[k]);
        snap.forEach(docSnap => {
          firestoreTrips[docSnap.id] = docSnap.data();
        });
        emitAggregated();
      }, (err) => console.warn('Firestore trips note:', err));
      unsubs.push(uF1);

      const colRequests = collection(firestore, 'ride_requests');
      const uF2 = onSnapshot(colRequests, (snap) => {
        Object.keys(firestoreRequests).forEach(k => delete firestoreRequests[k]);
        snap.forEach(docSnap => {
          firestoreRequests[docSnap.id] = docSnap.data();
        });
        emitAggregated();
      }, (err) => console.warn('Firestore ride_requests note:', err));
      unsubs.push(uF2);

      const colRides = collection(firestore, 'rides');
      const uF3 = onSnapshot(colRides, (snap) => {
        Object.keys(firestoreRides).forEach(k => delete firestoreRides[k]);
        snap.forEach(docSnap => {
          firestoreRides[docSnap.id] = docSnap.data();
        });
        emitAggregated();
      }, (err) => console.warn('Firestore rides note:', err));
      unsubs.push(uF3);
    } catch (e) {
      console.warn('Error setting up Firestore trip listeners:', e);
    }
  }

  return () => {
    unsubs.forEach(u => {
      try { u(); } catch { /* ignore */ }
    });
  };
}

/**
 * Toggle Rider / Passenger status in Firebase
 */
export async function toggleRiderAccountStatusInFirebase(
  riderId: string,
  newAccountStatus: PassengerAccountStatus
): Promise<boolean> {
  const { db, firestore } = initFirebaseService();
  let success = false;

  const operationalStatus: RiderStatus =
    newAccountStatus === 'SUSPENDED'
      ? 'suspended'
      : newAccountStatus === 'FLAGGED'
      ? 'flagged'
      : newAccountStatus === 'DEACTIVATED'
      ? 'deactivated'
      : newAccountStatus === 'INACTIVE'
      ? 'inactive'
      : 'active';

  if (db) {
    try {
      await update(ref(db, `riders/${riderId}`), {
        status: operationalStatus,
        accountStatus: newAccountStatus,
      });
      await update(ref(db, `users/${riderId}`), {
        status: operationalStatus,
        accountStatus: newAccountStatus,
      });
      success = true;
    } catch (err) {
      console.warn('RTDB toggle rider account status error:', err);
    }
  }

  if (firestore) {
    try {
      await updateDoc(doc(firestore, 'riders', riderId), {
        status: operationalStatus,
        accountStatus: newAccountStatus,
      });
      await updateDoc(doc(firestore, 'users', riderId), {
        status: operationalStatus,
        accountStatus: newAccountStatus,
      });
      success = true;
    } catch (err) {
      console.warn('Firestore toggle rider account status error:', err);
    }
  }

  return success;
}

export async function toggleRiderStatusInFirebase(
  riderId: string,
  newStatus: Rider['status'] | PassengerAccountStatus
): Promise<boolean> {
  let targetAccountStatus: PassengerAccountStatus = 'ACTIVE';
  if (newStatus === 'suspended' || newStatus === 'SUSPENDED') targetAccountStatus = 'SUSPENDED';
  else if (newStatus === 'flagged' || newStatus === 'FLAGGED') targetAccountStatus = 'FLAGGED';
  else if (newStatus === 'deactivated' || newStatus === 'DEACTIVATED') targetAccountStatus = 'DEACTIVATED';
  else if (newStatus === 'inactive' || newStatus === 'INACTIVE') targetAccountStatus = 'INACTIVE';
  else if (newStatus === 'ON_TRIP') targetAccountStatus = 'ON_TRIP';
  else targetAccountStatus = 'ACTIVE';

  return toggleRiderAccountStatusInFirebase(riderId, targetAccountStatus);
}

/**
 * Approve Driver Registration / Document in Realtime Database / Firestore
 */
export async function approveDriverRegistrationInFirebase(
  driverId: string,
  docId?: string
): Promise<boolean> {
  const { db, firestore } = initFirebaseService();
  let success = false;

  // 1. Update in Realtime Database
  if (db) {
    try {
      const driverRef = ref(db, `drivers/${driverId}`);
      await update(driverRef, {
        status: 'online',
        accountStatus: 'ACTIVE',
        verificationStatus: 'APPROVED',
        isOnline: true,
      });

      // Update documents in drivers/${driverId} if any
      const driverSnap = await get(driverRef);
      if (driverSnap.exists()) {
        const dVal = driverSnap.val();
        if (dVal && Array.isArray(dVal.documents)) {
          const updatedDocs = dVal.documents.map((dItem: any) => ({
            ...dItem,
            status: 'APPROVED',
            verificationStatus: 'APPROVED',
            rejectionReason: '',
            verifiedAt: new Date().toISOString()
          }));
          await update(driverRef, { documents: updatedDocs });
        }
      }

      await update(ref(db, `driver_verifications/${driverId}`), {
        accountStatus: 'ACTIVE',
        verificationStatus: 'APPROVED',
        status: 'APPROVED',
        isOnline: true,
      });

      const verifSnap = await get(ref(db, `driver_verifications/${driverId}`));
      if (verifSnap.exists()) {
        const vVal = verifSnap.val();
        if (vVal && Array.isArray(vVal.documents)) {
          const updatedDocs = vVal.documents.map((dItem: any) => ({
            ...dItem,
            status: 'APPROVED',
            verificationStatus: 'APPROVED',
            rejectionReason: '',
            verifiedAt: new Date().toISOString()
          }));
          await update(ref(db, `driver_verifications/${driverId}`), { documents: updatedDocs });
        }
      }

      await update(ref(db, `users/${driverId}`), {
        status: 'online',
        accountStatus: 'ACTIVE',
        verificationStatus: 'APPROVED',
        isOnline: true,
      });
      success = true;
    } catch (err) {
      console.warn('Error updating driver in RTDB:', err);
    }
  }

  // 2. Update in Firestore
  if (firestore) {
    try {
      const dRef = doc(firestore, 'drivers', driverId);
      await setDoc(dRef, {
        status: 'online',
        accountStatus: 'ACTIVE',
        verificationStatus: 'APPROVED',
        updatedAt: new Date().toISOString()
      }, { merge: true });

      const verifRef = doc(firestore, 'driver_verifications', driverId);
      await setDoc(verifRef, {
        accountStatus: 'ACTIVE',
        verificationStatus: 'APPROVED',
        status: 'APPROVED',
        updatedAt: new Date().toISOString()
      }, { merge: true });

      const uRef = doc(firestore, 'users', driverId);
      await setDoc(uRef, {
        status: 'online',
        accountStatus: 'ACTIVE',
        verificationStatus: 'APPROVED',
        updatedAt: new Date().toISOString()
      }, { merge: true });

      success = true;
    } catch (err) {
      console.warn('Error updating driver in Firestore:', err);
    }
  }

  return success;
}

/**
 * Update Driver Document Verification Status in Firebase
 */
export async function updateDriverDocumentInFirebase(
  driverId: string,
  docId: string,
  status: 'verified' | 'rejected',
  reason?: string
): Promise<boolean> {
  const { db, firestore } = initFirebaseService();
  let success = false;
  const firebaseDocStatus = status === 'verified' ? 'APPROVED' : 'REJECTED';

  if (db) {
    try {
      // 1. Update drivers/${driverId}
      const driverRef = ref(db, `drivers/${driverId}`);
      const driverSnap = await get(driverRef);
      if (driverSnap.exists()) {
        const dVal = driverSnap.val();
        if (dVal) {
          const updates: Record<string, any> = {};
          if (dVal.documents) {
            if (Array.isArray(dVal.documents)) {
              const updated = dVal.documents.map((docItem: any, idx: number) => {
                const matchKey = docItem.id || docItem.docType || docItem.type || `doc_${idx}`;
                if (matchKey === docId || docItem.docType === docId || docItem.id === docId || docItem.type === docId) {
                  return {
                    ...docItem,
                    status: firebaseDocStatus,
                    verificationStatus: firebaseDocStatus,
                    rejectionReason: reason || docItem.rejectionReason || ''
                  };
                }
                return docItem;
              });
              updates[`documents`] = updated;
              const allApproved = updated.length > 0 && updated.every((d: any) => {
                const s = (d.status || d.verificationStatus || '').toUpperCase();
                return s === 'APPROVED' || s === 'VERIFIED';
              });
              const anyRejected = updated.some((d: any) => {
                const s = (d.status || d.verificationStatus || '').toUpperCase();
                return s === 'REJECTED';
              });

              if (allApproved) {
                updates['verificationStatus'] = 'APPROVED';
                updates['status'] = 'online';
                updates['accountStatus'] = 'ACTIVE';
                updates['isOnline'] = true;
              } else if (anyRejected) {
                updates['verificationStatus'] = 'REJECTED';
              }
            } else if (typeof dVal.documents === 'object') {
              if (dVal.documents[docId]) {
                updates[`documents/${docId}/status`] = firebaseDocStatus;
                updates[`documents/${docId}/verificationStatus`] = firebaseDocStatus;
                updates[`documents/${docId}/rejectionReason`] = reason || '';
              }
            }
          }
          if (Object.keys(updates).length > 0) {
            await update(driverRef, updates);
          }
        }
      }

      // 2. Update driver_verifications/${driverId}
      const verifRef = ref(db, `driver_verifications/${driverId}`);
      const verifSnap = await get(verifRef);
      if (verifSnap.exists()) {
        const vVal = verifSnap.val();
        if (vVal) {
          const vUpdates: Record<string, any> = {};
          if (vVal.documents && Array.isArray(vVal.documents)) {
            const updated = vVal.documents.map((docItem: any, idx: number) => {
              const matchKey = docItem.id || docItem.docType || docItem.type || `doc_${idx}`;
              if (matchKey === docId || docItem.docType === docId || docItem.id === docId || docItem.type === docId) {
                return {
                  ...docItem,
                  status: firebaseDocStatus,
                  verificationStatus: firebaseDocStatus,
                  rejectionReason: reason || docItem.rejectionReason || ''
                };
              }
              return docItem;
            });
            vUpdates['documents'] = updated;
            const allApproved = updated.length > 0 && updated.every((d: any) => {
              const s = (d.status || d.verificationStatus || '').toUpperCase();
              return s === 'APPROVED' || s === 'VERIFIED';
            });
            const anyRejected = updated.some((d: any) => {
              const s = (d.status || d.verificationStatus || '').toUpperCase();
              return s === 'REJECTED';
            });

            if (allApproved) {
              vUpdates['status'] = 'APPROVED';
              vUpdates['accountStatus'] = 'ACTIVE';
              vUpdates['verificationStatus'] = 'APPROVED';
              vUpdates['isVerified'] = true;
            } else if (anyRejected) {
              vUpdates['verificationStatus'] = 'REJECTED';
              vUpdates['status'] = 'REJECTED';
            }
          }
          if (Object.keys(vUpdates).length > 0) {
            await update(verifRef, vUpdates);
          }
        }
      }

      // 3. Update users/${driverId}/driverVerification
      const userRef = ref(db, `users/${driverId}`);
      const userSnap = await get(userRef);
      if (userSnap.exists()) {
        const uVal = userSnap.val();
        if (uVal && uVal.driverVerification && uVal.driverVerification.documents) {
          if (Array.isArray(uVal.driverVerification.documents)) {
            const updated = uVal.driverVerification.documents.map((docItem: any, idx: number) => {
              const matchKey = docItem.id || docItem.docType || docItem.type || `doc_${idx}`;
              if (matchKey === docId || docItem.docType === docId || docItem.id === docId || docItem.type === docId) {
                return {
                  ...docItem,
                  status: firebaseDocStatus,
                  verificationStatus: firebaseDocStatus,
                  rejectionReason: reason || docItem.rejectionReason || ''
                };
              }
              return docItem;
            });
            const allApproved = updated.length > 0 && updated.every((d: any) => {
              const s = (d.status || d.verificationStatus || '').toUpperCase();
              return s === 'APPROVED' || s === 'VERIFIED';
            });
            const anyRejected = updated.some((d: any) => {
              const s = (d.status || d.verificationStatus || '').toUpperCase();
              return s === 'REJECTED';
            });

            const uUpdates: Record<string, any> = {
              'driverVerification/documents': updated,
            };
            if (allApproved) {
              uUpdates['verificationStatus'] = 'APPROVED';
              uUpdates['accountStatus'] = 'ACTIVE';
              uUpdates['status'] = 'online';
            } else if (anyRejected) {
              uUpdates['verificationStatus'] = 'REJECTED';
            }
            await update(ref(db, `users/${driverId}`), uUpdates);
          }
        }
      }

      success = true;
    } catch (err) {
      console.warn('Failed updating driver doc in RTDB:', err);
    }
  }

  if (firestore) {
    try {
      const verifRef = doc(firestore, 'driver_verifications', driverId);
      const verifDoc = await getDoc(verifRef);
      let updatedDocs: any[] = [];
      let allApproved = false;
      let anyRejected = false;

      if (verifDoc.exists()) {
        const data = verifDoc.data();
        if (data && Array.isArray(data.documents)) {
          updatedDocs = data.documents.map((docItem: any, idx: number) => {
            const matchKey = docItem.id || docItem.docType || docItem.type || `doc_${idx}`;
            if (matchKey === docId || docItem.docType === docId || docItem.id === docId || docItem.type === docId) {
              return {
                ...docItem,
                status: status === 'verified' ? 'verified' : 'rejected',
                verificationStatus: firebaseDocStatus,
                rejectionReason: reason || ''
              };
            }
            return docItem;
          });
          allApproved = updatedDocs.length > 0 && updatedDocs.every((d: any) => d.status === 'verified' || d.verificationStatus === 'APPROVED');
          anyRejected = updatedDocs.some((d: any) => d.status === 'rejected' || d.verificationStatus === 'REJECTED');
        }
      }

      await setDoc(verifRef, {
        documents: updatedDocs,
        status: allApproved ? 'APPROVED' : anyRejected ? 'REJECTED' : 'PENDING',
        verificationStatus: allApproved ? 'APPROVED' : anyRejected ? 'REJECTED' : 'PENDING',
        accountStatus: allApproved ? 'ACTIVE' : undefined,
        updatedAt: new Date().toISOString()
      }, { merge: true });

      const dRef = doc(firestore, 'drivers', driverId);
      await setDoc(dRef, {
        [`document_${docId}_status`]: status,
        documents: updatedDocs.length > 0 ? updatedDocs : undefined,
        verificationStatus: allApproved ? 'APPROVED' : anyRejected ? 'REJECTED' : undefined,
        status: allApproved ? 'online' : undefined,
        accountStatus: allApproved ? 'ACTIVE' : undefined,
        updatedAt: new Date().toISOString()
      }, { merge: true });

      success = true;
    } catch (err) {
      console.warn('Failed updating driver doc in Firestore:', err);
    }
  }

  return success;
}

/**
 * Toggle Driver Account Status (Active, Suspended, Flagged, etc.) in Firebase
 * Strictly ensures that accountStatus changes do NOT touch or reset verificationStatus.
 */
export async function toggleDriverAccountStatusInFirebase(
  driverId: string,
  newAccountStatus: DriverAccountStatus
): Promise<boolean> {
  const { db, firestore } = initFirebaseService();
  let success = false;

  const isSuspended = newAccountStatus === 'SUSPENDED';
  const isFlagged = newAccountStatus === 'FLAGGED';
  const isOnline = newAccountStatus === 'ONLINE' || newAccountStatus === 'ON_TRIP';
  const rtdbStatus = isSuspended ? 'suspended' : isFlagged ? 'flagged' : newAccountStatus === 'ON_TRIP' ? 'on_trip' : isOnline ? 'online' : newAccountStatus === 'PENDING_REVIEW' ? 'pending_verification' : 'offline';

  if (db) {
    try {
      // 1. Update drivers/$driverId (only accountStatus, status, isOnline - strictly DO NOT touch verificationStatus)
      await update(ref(db, `drivers/${driverId}`), {
        status: rtdbStatus,
        accountStatus: newAccountStatus,
        isOnline: isOnline && !isSuspended && !isFlagged
      });

      // 2. Update driver_verifications/$driverId (only accountStatus, isOnline - strictly DO NOT touch verificationStatus)
      await update(ref(db, `driver_verifications/${driverId}`), {
        accountStatus: newAccountStatus,
        isOnline: isOnline && !isSuspended && !isFlagged
      });

      // 3. Scan driver_verifications node to find any matching records by key, driverId, or id
      const verifRef = ref(db, 'driver_verifications');
      onValue(verifRef, (snapshot) => {
        const val = snapshot.val();
        if (val && typeof val === 'object') {
          Object.entries(val).forEach(async ([key, record]: [string, any]) => {
            if (key === driverId || record.driverId === driverId || record.id === driverId) {
              await update(ref(db, `driver_verifications/${key}`), {
                accountStatus: newAccountStatus,
                isOnline: isOnline && !isSuspended && !isFlagged
              });
            }
          });
        }
      }, { onlyOnce: true });

      // 4. Update users/$driverId
      await update(ref(db, `users/${driverId}`), {
        status: rtdbStatus,
        accountStatus: newAccountStatus,
        isOnline: isOnline && !isSuspended && !isFlagged
      });

      success = true;
    } catch (err) {
      console.warn('RTDB toggle driver account status error:', err);
    }
  }

  if (firestore) {
    try {
      await updateDoc(doc(firestore, 'drivers', driverId), {
        status: rtdbStatus,
        accountStatus: newAccountStatus
      });
      await updateDoc(doc(firestore, 'users', driverId), {
        status: rtdbStatus,
        accountStatus: newAccountStatus
      });
      await updateDoc(doc(firestore, 'driver_verifications', driverId), {
        accountStatus: newAccountStatus
      });
      success = true;
    } catch (err) {
      console.warn('Firestore toggle driver account status error:', err);
    }
  }

  return success;
}

/**
 * Toggle Driver Status in Firebase (backward-compatible adapter)
 */
export async function toggleDriverStatusInFirebase(
  driverId: string,
  newStatus: Driver['status'] | DriverAccountStatus
): Promise<boolean> {
  let targetAccountStatus: DriverAccountStatus = 'ACTIVE';
  const statusUpper = String(newStatus).toUpperCase();
  if (statusUpper === 'SUSPENDED') {
    targetAccountStatus = 'SUSPENDED';
  } else if (statusUpper === 'FLAGGED') {
    targetAccountStatus = 'FLAGGED';
  } else if (statusUpper === 'ONLINE') {
    targetAccountStatus = 'ONLINE';
  } else if (statusUpper === 'ON_TRIP') {
    targetAccountStatus = 'ON_TRIP';
  } else if (statusUpper === 'PENDING_VERIFICATION' || statusUpper === 'PENDING_REVIEW' || statusUpper === 'PENDING') {
    targetAccountStatus = 'PENDING_REVIEW';
  } else {
    targetAccountStatus = 'ACTIVE';
  }

  return toggleDriverAccountStatusInFirebase(driverId, targetAccountStatus);
}

/**
 * Adjust Driver Wallet in Firebase & Record Transaction Audit
 */
export async function adjustDriverWalletInFirebase(
  driverId: string,
  newBalance: number,
  transaction?: Partial<WalletTransaction>
): Promise<boolean> {
  const { db, firestore } = initFirebaseService();
  let success = false;

  const walletPayload = {
    walletBalance: newBalance,
    balance: newBalance,
    wallet_balance: newBalance,
    lastWalletAdjustment: new Date().toISOString()
  };

  if (db) {
    try {
      await update(ref(db, `drivers/${driverId}`), walletPayload);
      await update(ref(db, `users/${driverId}`), walletPayload);
      await set(ref(db, `wallets/${driverId}`), {
        balance: newBalance,
        walletBalance: newBalance,
        updatedAt: new Date().toISOString()
      });
      success = true;
    } catch (err) {
      console.warn('RTDB adjust driver wallet error:', err);
    }
  }

  if (firestore) {
    try {
      await setDoc(doc(firestore, 'drivers', driverId), walletPayload, { merge: true });
      await setDoc(doc(firestore, 'users', driverId), walletPayload, { merge: true });
      await setDoc(doc(firestore, 'wallets', driverId), {
        balance: newBalance,
        walletBalance: newBalance,
        updatedAt: new Date().toISOString()
      }, { merge: true });
      success = true;
    } catch (err) {
      console.warn('Firestore adjust driver wallet error:', err);
    }
  }

  if (transaction) {
    await recordWalletTransactionInFirebase({
      id: transaction.id || `tx_drv_${Date.now()}_${Math.floor(100 + Math.random() * 900)}`,
      userType: 'driver',
      userId: driverId,
      userName: transaction.userName || 'Driver',
      userPhone: transaction.userPhone || '',
      type: transaction.type || 'admin_adjustment',
      amount: transaction.amount || 0,
      previousBalance: transaction.previousBalance || 0,
      newBalance: newBalance,
      currency: 'PKR',
      paymentMethod: transaction.paymentMethod || 'admin_manual',
      reason: transaction.reason || 'Manual administrative balance adjustment',
      referenceId: transaction.referenceId,
      administeredBy: transaction.administeredBy || 'Admin Ops Manager',
      timestamp: transaction.timestamp || new Date().toISOString()
    });
  }

  return success;
}

/**
 * Adjust Rider / Passenger Wallet in Firebase & Record Transaction Audit
 */
export async function adjustRiderWalletInFirebase(
  riderId: string,
  newBalance: number,
  transaction?: Partial<WalletTransaction>
): Promise<boolean> {
  const { db, firestore } = initFirebaseService();
  let success = false;

  const walletPayload = {
    walletBalance: newBalance,
    balance: newBalance,
    wallet_balance: newBalance,
    lastWalletAdjustment: new Date().toISOString()
  };

  if (db) {
    try {
      await update(ref(db, `riders/${riderId}`), walletPayload);
      await update(ref(db, `users/${riderId}`), walletPayload);
      await set(ref(db, `wallets/${riderId}`), {
        balance: newBalance,
        walletBalance: newBalance,
        updatedAt: new Date().toISOString()
      });
      success = true;
    } catch (err) {
      console.warn('RTDB adjust rider wallet error:', err);
    }
  }

  if (firestore) {
    try {
      await setDoc(doc(firestore, 'riders', riderId), walletPayload, { merge: true });
      await setDoc(doc(firestore, 'users', riderId), walletPayload, { merge: true });
      await setDoc(doc(firestore, 'wallets', riderId), {
        balance: newBalance,
        walletBalance: newBalance,
        updatedAt: new Date().toISOString()
      }, { merge: true });
      success = true;
    } catch (err) {
      console.warn('Firestore adjust rider wallet error:', err);
    }
  }

  if (transaction) {
    await recordWalletTransactionInFirebase({
      id: transaction.id || `tx_rdr_${Date.now()}_${Math.floor(100 + Math.random() * 900)}`,
      userType: 'rider',
      userId: riderId,
      userName: transaction.userName || 'Passenger',
      userPhone: transaction.userPhone || '',
      type: transaction.type || 'topup',
      amount: transaction.amount || 0,
      previousBalance: transaction.previousBalance || 0,
      newBalance: newBalance,
      currency: 'PKR',
      paymentMethod: transaction.paymentMethod || 'admin_manual',
      reason: transaction.reason || 'Passenger wallet top-up / adjustment',
      referenceId: transaction.referenceId,
      administeredBy: transaction.administeredBy || 'Admin Ops Manager',
      timestamp: transaction.timestamp || new Date().toISOString()
    });
  }

  return success;
}

/**
 * Record a Wallet Transaction Audit Entry in Firebase
 */
export async function recordWalletTransactionInFirebase(
  transaction: WalletTransaction
): Promise<boolean> {
  const { db, firestore } = initFirebaseService();
  let success = false;

  if (db) {
    try {
      await set(ref(db, `wallet_transactions/${transaction.id}`), transaction);
      success = true;
    } catch (err) {
      console.warn('RTDB record wallet transaction error:', err);
    }
  }

  if (firestore) {
    try {
      await setDoc(doc(firestore, 'wallet_transactions', transaction.id), transaction);
      success = true;
    } catch (err) {
      console.warn('Firestore record wallet transaction error:', err);
    }
  }

  return success;
}

/**
 * Real-time listener for Wallet Transactions (/wallet_transactions across RTDB & Firestore)
 */
export function subscribeToWalletTransactions(
  callback: (transactions: WalletTransaction[]) => void
): () => void {
  const { db, firestore } = initFirebaseService();
  const unsubs: Array<() => void> = [];
  const rtdbTxMap: Record<string, WalletTransaction> = {};
  const firestoreTxMap: Record<string, WalletTransaction> = {};

  const emitAggregated = () => {
    const combinedMap: Record<string, WalletTransaction> = {
      ...rtdbTxMap,
      ...firestoreTxMap,
    };
    const list = Object.values(combinedMap).sort((a, b) =>
      String(b.timestamp || '') > String(a.timestamp || '') ? 1 : -1
    );
    callback(list);
  };

  if (db) {
    try {
      const txRef = ref(db, 'wallet_transactions');
      const u1 = onValue(
        txRef,
        (snapshot) => {
          const val = snapshot.val();
          Object.keys(rtdbTxMap).forEach((k) => delete rtdbTxMap[k]);
          if (val && typeof val === 'object') {
            Object.entries(val).forEach(([key, tx]: [string, any]) => {
              if (tx && typeof tx === 'object') {
                rtdbTxMap[key] = { id: key, ...tx };
              }
            });
          }
          emitAggregated();
        },
        (err) => {
          console.warn('RTDB wallet_transactions error:', err);
        }
      );
      unsubs.push(u1);
    } catch (err) {
      console.warn('RTDB wallet_transactions error:', err);
    }
  }

  if (firestore) {
    try {
      const colRef = collection(firestore, 'wallet_transactions');
      const u2 = onSnapshot(
        colRef,
        (snapshot) => {
          Object.keys(firestoreTxMap).forEach((k) => delete firestoreTxMap[k]);
          snapshot.forEach((docSnap) => {
            firestoreTxMap[docSnap.id] = { id: docSnap.id, ...docSnap.data() } as WalletTransaction;
          });
          emitAggregated();
        },
        (err) => {
          console.warn('Firestore wallet_transactions error:', err);
        }
      );
      unsubs.push(u2);
    } catch (err) {
      console.warn('Firestore wallet_transactions error:', err);
    }
  }

  if (unsubs.length === 0) {
    callback([]);
    return () => {};
  }

  return () => {
    unsubs.forEach((u) => {
      try { u(); } catch { /* ignore */ }
    });
  };
}

/**
 * Create a new Payout Request in Firebase (on behalf of a driver or from driver app)
 */
export async function createPayoutRequestInFirebase(
  payoutData: Partial<PayoutRequest>
): Promise<string> {
  const { db, firestore } = initFirebaseService();
  const payoutId = payoutData.id || `pay_${Date.now()}_${Math.floor(100 + Math.random() * 900)}`;

  const fullPayout: PayoutRequest = {
    id: payoutId,
    driverId: payoutData.driverId || '',
    driverName: payoutData.driverName || 'Driver',
    driverPhone: payoutData.driverPhone || '',
    driverAvatar: payoutData.driverAvatar || '',
    amount: payoutData.amount || 0,
    currency: 'PKR',
    paymentMethod: payoutData.paymentMethod || 'jazzcash',
    accountTitle: payoutData.accountTitle || payoutData.driverName || 'Driver Account',
    accountNumber: payoutData.accountNumber || payoutData.driverPhone || '0300-0000000',
    bankName: payoutData.bankName || '',
    status: payoutData.status || 'pending',
    requestedAt: payoutData.requestedAt || new Date().toISOString(),
    gatewayFee: payoutData.gatewayFee || 20,
    note: payoutData.note || 'Driver mobile withdrawal'
  };

  if (db) {
    try {
      await set(ref(db, `payout_requests/${payoutId}`), fullPayout);
    } catch (err) {
      console.warn('RTDB create payout request error:', err);
    }
  }

  if (firestore) {
    try {
      await setDoc(doc(firestore, 'payout_requests', payoutId), fullPayout);
    } catch (err) {
      console.warn('Firestore create payout request error:', err);
    }
  }

  return payoutId;
}

/**
 * Real-time listener for safety_reports_admin node
 */
export function subscribeToSafetyReports(
  callback: (reports: SafetyReport[]) => void
): () => void {
  const { db, firestore } = initFirebaseService();

  if (db) {
    const safetyRef = ref(db, 'safety_reports_admin');
    const unsub = onValue(safetyRef, (snapshot) => {
      const val = snapshot.val();
      const list: SafetyReport[] = [];
      if (val && typeof val === 'object') {
        Object.entries(val).forEach(([key, record]: [string, any]) => {
          if (record && typeof record === 'object') {
            list.push({
              id: key,
              rideId: record.rideId || '',
              category: record.category || '',
              categoryLabel: record.categoryLabel || record.category || 'Safety Alert',
              description: record.description || '',
              reporterId: record.reporterId || '',
              reporterName: record.reporterName || 'Anonymous',
              reporterPhone: record.reporterPhone || '',
              reporterRole: record.reporterRole || 'PASSENGER',
              reportedUserId: record.reportedUserId || '',
              reportedUserName: record.reportedUserName || 'Driver',
              reportedUserRole: record.reportedUserRole || 'DRIVER',
              ridePickupTitle: record.ridePickupTitle || '',
              rideDestinationTitle: record.rideDestinationTitle || '',
              driverPlateNumber: record.driverPlateNumber || '',
              status: record.status || 'PENDING_ADMIN_REVIEW',
              timestamp: typeof record.timestamp === 'number' ? record.timestamp : Date.now(),
              blockUser: record.blockUser !== false
            });
          }
        });
      }
      callback(list);
    }, (err) => {
      console.warn('RTDB safety_reports_admin listener note:', err);
    });
    return () => unsub();
  }

  if (firestore) {
    const colRef = collection(firestore, 'safety_reports_admin');
    const unsub = onSnapshot(colRef, (snapshot) => {
      const list: SafetyReport[] = [];
      snapshot.forEach(docSnap => {
        const record = docSnap.data();
        list.push({
          id: docSnap.id,
          rideId: record.rideId || '',
          category: record.category || '',
          categoryLabel: record.categoryLabel || record.category || 'Safety Alert',
          description: record.description || '',
          reporterId: record.reporterId || '',
          reporterName: record.reporterName || 'Anonymous',
          reporterPhone: record.reporterPhone || '',
          reporterRole: record.reporterRole || 'PASSENGER',
          reportedUserId: record.reportedUserId || '',
          reportedUserName: record.reportedUserName || 'Driver',
          reportedUserRole: record.reportedUserRole || 'DRIVER',
          ridePickupTitle: record.ridePickupTitle || '',
          rideDestinationTitle: record.rideDestinationTitle || '',
          driverPlateNumber: record.driverPlateNumber || '',
          status: record.status || 'PENDING_ADMIN_REVIEW',
          timestamp: typeof record.timestamp === 'number' ? record.timestamp : Date.now(),
          blockUser: record.blockUser !== false
        });
      });
      callback(list);
    }, (err) => {
      console.warn('Firestore safety_reports_admin listener note:', err);
    });
    return () => unsub();
  }

  return () => {};
}

/**
 * Update Safety Report status in Firebase RTDB / Firestore
 */
export async function updateSafetyReportStatusInFirebase(
  reportId: string,
  newStatus: string
): Promise<boolean> {
  const { db, firestore } = initFirebaseService();
  let success = false;

  if (db) {
    try {
      await update(ref(db, `safety_reports_admin/${reportId}`), {
        status: newStatus
      });
      success = true;
    } catch (err) {
      console.warn('RTDB update safety report status error:', err);
    }
  }

  if (firestore) {
    try {
      await updateDoc(doc(firestore, 'safety_reports_admin', reportId), {
        status: newStatus
      });
      success = true;
    } catch (err) {
      console.warn('Firestore update safety report status error:', err);
    }
  }

  return success;
}

/**
 * Real-time listener for Ride Ratings & Reviews (/ride_ratings)
 */
export function subscribeToRideRatings(callback: (ratings: RideRating[]) => void): () => void {
  const { db, firestore } = initFirebaseService();

  if (db) {
    const ratingsRef = ref(db, 'ride_ratings');
    const unsub = onValue(ratingsRef, (snapshot) => {
      const val = snapshot.val();
      const list: RideRating[] = [];
      if (val && typeof val === 'object') {
        Object.entries(val).forEach(([key, r]: [string, any]) => {
          if (r) list.push({ id: key, ...r });
        });
      }
      callback(list);
    }, (err) => {
      console.warn('RTDB ride_ratings listener note:', err);
      callback([]);
    });
    return () => unsub();
  }

  if (firestore) {
    const colRef = collection(firestore, 'ride_ratings');
    const unsub = onSnapshot(colRef, (snapshot) => {
      const list: RideRating[] = [];
      snapshot.forEach(docSnap => {
        list.push({ id: docSnap.id, ...docSnap.data() } as RideRating);
      });
      callback(list);
    }, (err) => {
      console.warn('Firestore ride_ratings listener note:', err);
      callback([]);
    });
    return () => unsub();
  }

  callback([]);
  return () => {};
}

/**
 * Update Ride Rating & Review details in Firebase RTDB / Firestore
 */
export async function updateRideRatingInFirebase(
  ratingId: string,
  updates: Partial<RideRating>
): Promise<boolean> {
  const { db, firestore } = initFirebaseService();
  let success = false;
  const updateData: Record<string, any> = {
    ...updates,
    updatedAt: new Date().toISOString()
  };

  if (db) {
    try {
      await update(ref(db, `ride_ratings/${ratingId}`), updateData);
      success = true;
    } catch (err) {
      console.warn('RTDB update ride rating error:', err);
    }
  }

  if (firestore) {
    try {
      await updateDoc(doc(firestore, 'ride_ratings', ratingId), updateData);
      success = true;
    } catch (err) {
      console.warn('Firestore update ride rating error:', err);
    }
  }

  return success;
}

/**
 * Toggle Suspicious/Flagged status of a Ride Rating in Firebase
 */
export async function toggleRatingSuspiciousInFirebase(
  ratingId: string,
  isSuspicious: boolean,
  reason?: string,
  adminNote?: string,
  adminUser?: string
): Promise<boolean> {
  const updates: Partial<RideRating> = {
    isSuspicious,
    flagReason: isSuspicious ? (reason || 'Flagged by Admin for Review') : '',
    moderationStatus: isSuspicious ? 'flagged' : 'published',
    adminNote: adminNote || '',
    moderatedBy: adminUser || 'Admin Ops Manager',
    moderatedAt: new Date().toISOString()
  };
  return updateRideRatingInFirebase(ratingId, updates);
}

/**
 * Delete / Remove a Ride Rating from Firebase
 */
export async function deleteRideRatingInFirebase(ratingId: string): Promise<boolean> {
  const { db, firestore } = initFirebaseService();
  let success = false;

  if (db) {
    try {
      await remove(ref(db, `ride_ratings/${ratingId}`));
      success = true;
    } catch (err) {
      console.warn('RTDB delete ride rating error:', err);
    }
  }

  if (firestore) {
    try {
      await deleteDoc(doc(firestore, 'ride_ratings', ratingId));
      success = true;
    } catch (err) {
      console.warn('Firestore delete ride rating error:', err);
    }
  }

  return success;
}

/**
 * Real-time listener for Admin Notifications (/admin_notifications)
 */
export function subscribeToAdminNotifications(callback: (notifications: AdminNotification[]) => void): () => void {
  const { db, firestore } = initFirebaseService();

  if (db) {
    const notifsRef = ref(db, 'admin_notifications');
    const unsub = onValue(notifsRef, (snapshot) => {
      const val = snapshot.val();
      const list: AdminNotification[] = [];
      if (val && typeof val === 'object') {
        Object.entries(val).forEach(([key, n]: [string, any]) => {
          if (n) list.push({ id: key, ...n });
        });
      }
      callback(list);
    }, (err) => {
      console.warn('RTDB admin_notifications listener note:', err);
      callback([]);
    });
    return () => unsub();
  }

  if (firestore) {
    const colRef = collection(firestore, 'admin_notifications');
    const unsub = onSnapshot(colRef, (snapshot) => {
      const list: AdminNotification[] = [];
      snapshot.forEach(docSnap => {
        list.push({ id: docSnap.id, ...docSnap.data() } as AdminNotification);
      });
      callback(list);
    }, (err) => {
      console.warn('Firestore admin_notifications listener note:', err);
      callback([]);
    });
    return () => unsub();
  }

  callback([]);
  return () => {};
}

export async function markNotificationAsReadInFirebase(notificationId: string): Promise<boolean> {
  const { db, firestore } = initFirebaseService();
  let success = false;

  if (db) {
    try {
      await update(ref(db, `admin_notifications/${notificationId}`), { isRead: true });
      success = true;
    } catch (err) {
      console.warn('RTDB mark notification read error:', err);
    }
  }

  if (firestore) {
    try {
      await updateDoc(doc(firestore, 'admin_notifications', notificationId), { isRead: true });
      success = true;
    } catch (err) {
      console.warn('Firestore mark notification read error:', err);
    }
  }

  return success;
}

export async function markAllNotificationsAsReadInFirebase(): Promise<boolean> {
  const { db, firestore } = initFirebaseService();
  let success = false;

  if (db) {
    try {
      const notifsRef = ref(db, 'admin_notifications');
      const snap = await get(notifsRef);
      const val = snap.val();
      if (val && typeof val === 'object') {
        const updates: Record<string, any> = {};
        Object.keys(val).forEach(key => {
          updates[`${key}/isRead`] = true;
        });
        await update(ref(db, 'admin_notifications'), updates);
        success = true;
      }
    } catch (err) {
      console.warn('RTDB mark all read error:', err);
    }
  }

  if (firestore) {
    try {
      const colRef = collection(firestore, 'admin_notifications');
      const snap = await getDocs(colRef);
      const batch = writeBatch(firestore);
      snap.forEach(docSnap => {
        batch.update(docSnap.ref, { isRead: true });
      });
      await batch.commit();
      success = true;
    } catch (err) {
      console.warn('Firestore mark all read error:', err);
    }
  }

  return success;
}

// ---------------------------------------------------------------------------
// Pricing Configs: Realtime Database & Firestore Sync
// ---------------------------------------------------------------------------
export function subscribeToPricingConfigs(callback: (configs: PricingConfig[]) => void): () => void {
  const { db, firestore } = initFirebaseService();
  let unsubFirestore: FirestoreUnsubscribe | null = null;
  let unsubRTDB: RTDBUnsubscribe | null = null;

  if (db) {
    const pricingRef = ref(db, 'pricingConfigs');
    unsubRTDB = onValue(pricingRef, (snapshot) => {
      const val = snapshot.val();
      if (val && typeof val === 'object') {
        const list: PricingConfig[] = Object.entries(val).map(([key, v]: [string, any]) => ({
          vehicleType: (v?.vehicleType || key) as any,
          name: v?.name || key,
          baseFare: Number(v?.baseFare) || 0,
          perKmRate: Number(v?.perKmRate) || 0,
          perMinuteRate: Number(v?.perMinuteRate) || 0,
          minimumFare: Number(v?.minimumFare) || 0,
          commissionPercentage: Number(v?.commissionPercentage) || 18,
          cancellationFee: Number(v?.cancellationFee) || 0,
          currency: v?.currency || 'Rs.',
          ...v,
        }));
        if (list.length > 0) {
          callback(list);
          return;
        }
      }
      callback(DEFAULT_PRICING_CONFIGS);
    }, (err) => {
      console.warn('RTDB subscribeToPricingConfigs error:', err);
      callback(DEFAULT_PRICING_CONFIGS);
    });
  } else if (firestore) {
    const colRef = collection(firestore, 'pricingConfigs');
    unsubFirestore = onSnapshot(colRef, (snapshot) => {
      if (!snapshot.empty) {
        const list: PricingConfig[] = [];
        snapshot.forEach((docSnap) => {
          const data = docSnap.data();
          list.push({
            vehicleType: (data?.vehicleType || docSnap.id) as any,
            name: data?.name || docSnap.id,
            baseFare: Number(data?.baseFare) || 0,
            perKmRate: Number(data?.perKmRate) || 0,
            perMinuteRate: Number(data?.perMinuteRate) || 0,
            minimumFare: Number(data?.minimumFare) || 0,
            commissionPercentage: Number(data?.commissionPercentage) || 18,
            cancellationFee: Number(data?.cancellationFee) || 0,
            currency: data?.currency || 'Rs.',
            ...data,
          } as PricingConfig);
        });
        callback(list);
      } else {
        callback(DEFAULT_PRICING_CONFIGS);
      }
    }, (err) => {
      console.warn('Firestore subscribeToPricingConfigs error:', err);
      callback(DEFAULT_PRICING_CONFIGS);
    });
  } else {
    callback(DEFAULT_PRICING_CONFIGS);
  }

  return () => {
    if (unsubRTDB) unsubRTDB();
    if (unsubFirestore) unsubFirestore();
  };
}

export async function updatePricingConfigInFirebase(updated: PricingConfig): Promise<boolean> {
  const { db, firestore } = initFirebaseService();
  let success = false;

  if (db) {
    try {
      await update(ref(db, `pricingConfigs/${updated.vehicleType}`), updated);
      success = true;
    } catch (err) {
      console.warn('RTDB updatePricingConfig error:', err);
    }
  }

  if (firestore) {
    try {
      await setDoc(doc(firestore, 'pricingConfigs', updated.vehicleType), updated, { merge: true });
      success = true;
    } catch (err) {
      console.warn('Firestore updatePricingConfig error:', err);
    }
  }

  return success;
}

// ---------------------------------------------------------------------------
// Surge Zones: Realtime Database & Firestore Sync
// ---------------------------------------------------------------------------
export function subscribeToSurgeZones(callback: (zones: SurgeZone[]) => void): () => void {
  const { db, firestore } = initFirebaseService();
  let unsubFirestore: FirestoreUnsubscribe | null = null;
  let unsubRTDB: RTDBUnsubscribe | null = null;

  if (db) {
    const surgeRef = ref(db, 'surgeZones');
    unsubRTDB = onValue(surgeRef, (snapshot) => {
      const val = snapshot.val();
      if (val && typeof val === 'object') {
        const list: SurgeZone[] = Object.entries(val).map(([key, v]: [string, any]) => ({
          id: v?.id || key,
          name: v?.name || 'Surge Zone',
          surgeMultiplier: Number(v?.surgeMultiplier) || 1.0,
          activeDemand: Number(v?.activeDemand) || 0,
          availableDrivers: Number(v?.availableDrivers) || 0,
          polygonCoordinates: v?.polygonCoordinates || [],
          center: v?.center || { lat: 33.6844, lng: 73.0479 },
          ...v,
        }));
        callback(list);
        return;
      }
      callback([]);
    }, (err) => {
      console.warn('RTDB subscribeToSurgeZones error:', err);
      callback([]);
    });
  } else if (firestore) {
    const colRef = collection(firestore, 'surgeZones');
    unsubFirestore = onSnapshot(colRef, (snapshot) => {
      const list: SurgeZone[] = [];
      snapshot.forEach((docSnap) => {
        const data = docSnap.data();
        list.push({
          id: data?.id || docSnap.id,
          name: data?.name || docSnap.id,
          surgeMultiplier: Number(data?.surgeMultiplier) || 1.0,
          activeDemand: Number(data?.activeDemand) || 0,
          availableDrivers: Number(data?.availableDrivers) || 0,
          polygonCoordinates: data?.polygonCoordinates || [],
          center: data?.center || { lat: 33.6844, lng: 73.0479 },
          ...data,
        } as unknown as SurgeZone);
      });
      callback(list);
    }, (err) => {
      console.warn('Firestore subscribeToSurgeZones error:', err);
      callback([]);
    });
  } else {
    callback([]);
  }

  return () => {
    if (unsubRTDB) unsubRTDB();
    if (unsubFirestore) unsubFirestore();
  };
}

export async function updateSurgeZoneInFirebase(zoneId: string, multiplier: number): Promise<boolean> {
  const { db, firestore } = initFirebaseService();
  let success = false;

  if (db) {
    try {
      await update(ref(db, `surgeZones/${zoneId}`), {
        surgeMultiplier: multiplier,
        status: multiplier > 1.0 ? 'active' : 'disabled'
      });
      success = true;
    } catch (err) {
      console.warn('RTDB updateSurgeZone error:', err);
    }
  }

  if (firestore) {
    try {
      await updateDoc(doc(firestore, 'surgeZones', zoneId), {
        surgeMultiplier: multiplier,
        status: multiplier > 1.0 ? 'active' : 'disabled'
      });
      success = true;
    } catch (err) {
      console.warn('Firestore updateSurgeZone error:', err);
    }
  }

  return success;
}

// ---------------------------------------------------------------------------
// Driver Payout Requests: Realtime Database & Firestore Sync
// ---------------------------------------------------------------------------
export function subscribeToPayoutRequests(callback: (payouts: PayoutRequest[]) => void): () => void {
  const { db, firestore } = initFirebaseService();
  let unsubFirestore: FirestoreUnsubscribe | null = null;
  let unsubRTDB: RTDBUnsubscribe | null = null;

  if (db) {
    const payoutRef = ref(db, 'payout_requests');
    unsubRTDB = onValue(payoutRef, (snapshot) => {
      const val = snapshot.val();
      if (val && typeof val === 'object') {
        const list: PayoutRequest[] = Object.entries(val).map(([k, v]: [string, any]) => ({
          ...v,
          id: v?.id || k,
        }));
        callback(list.sort((a, b) => (String(b.requestedAt || '') > String(a.requestedAt || '') ? 1 : -1)));
        return;
      }
      callback([]);
    }, (err) => {
      console.warn('RTDB subscribeToPayoutRequests error:', err);
      callback([]);
    });
  } else if (firestore) {
    const colRef = collection(firestore, 'payout_requests');
    unsubFirestore = onSnapshot(colRef, (snapshot) => {
      const list: PayoutRequest[] = [];
      snapshot.forEach((docSnap) => {
        const data = docSnap.data() as PayoutRequest;
        list.push({ ...data, id: data.id || docSnap.id });
      });
      callback(list.sort((a, b) => (String(b.requestedAt || '') > String(a.requestedAt || '') ? 1 : -1)));
    }, (err) => {
      console.warn('Firestore subscribeToPayoutRequests error:', err);
      callback([]);
    });
  } else {
    callback([]);
  }

  return () => {
    if (unsubRTDB) unsubRTDB();
    if (unsubFirestore) unsubFirestore();
  };
}

export async function updatePayoutRequestInFirebase(
  payoutId: string,
  status: PayoutRequest['status'],
  processedBy: string = 'Admin (Ops Manager)',
  transactionRef?: string,
  rejectionReason?: string
): Promise<boolean> {
  const { db, firestore } = initFirebaseService();
  let success = false;
  const updateData: Record<string, any> = {
    status,
    processedAt: new Date().toISOString(),
    processedBy
  };
  if (transactionRef) updateData.transactionRef = transactionRef;
  if (rejectionReason) updateData.rejectionReason = rejectionReason;

  if (db) {
    try {
      await update(ref(db, `payout_requests/${payoutId}`), updateData);
      success = true;
    } catch (err) {
      console.warn('RTDB updatePayoutRequest error:', err);
    }
  }

  if (firestore) {
    try {
      await updateDoc(doc(firestore, 'payout_requests', payoutId), updateData);
      success = true;
    } catch (err) {
      console.warn('Firestore updatePayoutRequest error:', err);
    }
  }

  return success;
}

// ---------------------------------------------------------------------------
// Operational Recommendations: Realtime Database & Firestore Sync
// ---------------------------------------------------------------------------
export function subscribeToOperationalRecommendations(
  callback: (recommendations: OperationalRecommendation[]) => void
): () => void {
  const { db, firestore } = initFirebaseService();
  let unsubFirestore: FirestoreUnsubscribe | null = null;
  let unsubRTDB: RTDBUnsubscribe | null = null;

  if (db) {
    const recRef = ref(db, 'operational_recommendations');
    unsubRTDB = onValue(
      recRef,
      (snapshot) => {
        const val = snapshot.val();
        const list: OperationalRecommendation[] = [];
        if (val && typeof val === 'object') {
          Object.entries(val).forEach(([key, r]: [string, any]) => {
            if (r && typeof r === 'object') {
              list.push({ id: key, ...r });
            }
          });
        }
        callback(list);
      },
      (err) => {
        console.warn('RTDB subscribeToOperationalRecommendations error:', err);
        callback([]);
      }
    );
  } else if (firestore) {
    const colRef = collection(firestore, 'operational_recommendations');
    unsubFirestore = onSnapshot(
      colRef,
      (snapshot) => {
        const list: OperationalRecommendation[] = [];
        snapshot.forEach((docSnap) => {
          list.push({ id: docSnap.id, ...docSnap.data() } as OperationalRecommendation);
        });
        callback(list);
      },
      (err) => {
        console.warn('Firestore subscribeToOperationalRecommendations error:', err);
        callback([]);
      }
    );
  } else {
    callback([]);
  }

  return () => {
    if (unsubRTDB) unsubRTDB();
    if (unsubFirestore) unsubFirestore();
  };
}

export async function saveOperationalRecommendationToFirebase(
  rec: OperationalRecommendation
): Promise<boolean> {
  const { db, firestore } = initFirebaseService();
  let success = false;

  if (db) {
    try {
      await update(ref(db, `operational_recommendations/${rec.id}`), rec);
      success = true;
    } catch (err) {
      console.warn('RTDB saveOperationalRecommendation error:', err);
    }
  }

  if (firestore) {
    try {
      await setDoc(doc(firestore, 'operational_recommendations', rec.id), rec, { merge: true });
      success = true;
    } catch (err) {
      console.warn('Firestore saveOperationalRecommendation error:', err);
    }
  }

  return success;
}

export async function executeOperationalRecommendationInFirebase(
  recId: string,
  actionType: string,
  actionPayload?: any
): Promise<boolean> {
  const { db, firestore } = initFirebaseService();
  let success = false;
  const updateData = {
    executed: true,
    executedAt: new Date().toISOString(),
    executedBy: 'Admin (Autonomous Ops Center)',
    actionPayload: actionPayload || null
  };

  if (db) {
    try {
      await update(ref(db, `operational_recommendations/${recId}`), updateData);
      success = true;
    } catch (err) {
      console.warn('RTDB executeOperationalRecommendation error:', err);
    }
  }

  if (firestore) {
    try {
      await updateDoc(doc(firestore, 'operational_recommendations', recId), updateData);
      success = true;
    } catch (err) {
      console.warn('Firestore executeOperationalRecommendation error:', err);
    }
  }

  // Record action into system memory
  await recordSystemMemoryEvent({
    type: 'trip_assigned',
    title: `Autonomous Action Executed: ${actionType.replace(/_/g, ' ').toUpperCase()}`,
    description: `Recommendation #${recId.slice(0, 8)} executed by operations team. Live fleet synced.`,
    severity: 'success',
    timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  });

  return success;
}

// ---------------------------------------------------------------------------
// System Memory & Live Database State Mutation Stream
// ---------------------------------------------------------------------------
export async function recordSystemMemoryEvent(
  item: Omit<LiveActivityFeedItem, 'id'> & { id?: string }
): Promise<boolean> {
  const { db, firestore } = initFirebaseService();
  let success = false;
  const eventId = item.id || `sys-mem-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`;
  const record = {
    id: eventId,
    timestamp: item.timestamp || new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    type: item.type,
    title: item.title,
    description: item.description,
    severity: item.severity,
    createdAt: Date.now(),
    ...(item.tripId ? { tripId: item.tripId } : {}),
    ...(item.driverId ? { driverId: item.driverId } : {}),
    ...(item.riderId ? { riderId: item.riderId } : {})
  };

  if (db) {
    try {
      await update(ref(db, `system_memory/${eventId}`), record);
      success = true;
    } catch (err) {
      console.warn('RTDB recordSystemMemoryEvent error:', err);
    }
  }

  if (firestore) {
    try {
      await setDoc(doc(firestore, 'system_memory', eventId), record, { merge: true });
      success = true;
    } catch (err) {
      console.warn('Firestore recordSystemMemoryEvent error:', err);
    }
  }

  return success;
}

export function subscribeToSystemMemory(
  callback: (items: LiveActivityFeedItem[]) => void
): () => void {
  const { db, firestore } = initFirebaseService();

  if (db) {
    let memoryEvents: LiveActivityFeedItem[] = [];
    let requestEvents: LiveActivityFeedItem[] = [];
    let safetyEvents: LiveActivityFeedItem[] = [];
    let driverEvents: LiveActivityFeedItem[] = [];
    let payoutEvents: LiveActivityFeedItem[] = [];

    const emitAggregated = () => {
      const combined = [
        ...memoryEvents,
        ...requestEvents,
        ...safetyEvents,
        ...driverEvents,
        ...payoutEvents
      ];
      // Deduplicate by ID
      const seen = new Set<string>();
      const deduped: LiveActivityFeedItem[] = [];
      for (const item of combined) {
        if (!seen.has(item.id)) {
          seen.add(item.id);
          deduped.push(item);
        }
      }
      callback(deduped);
    };

    // 1. Explicit system memory
    const memRef = ref(db, 'system_memory');
    const unsubMem = onValue(memRef, (snap) => {
      const val = snap.val();
      const list: LiveActivityFeedItem[] = [];
      if (val && typeof val === 'object') {
        Object.entries(val).forEach(([key, r]: [string, any]) => {
          if (r && typeof r === 'object') {
            list.unshift({
              id: key,
              type: r.type || 'trip_request',
              title: r.title || 'System State Mutation',
              description: r.description || '',
              severity: r.severity || 'info',
              timestamp: r.timestamp || 'Live',
              tripId: r.tripId,
              driverId: r.driverId,
              riderId: r.riderId
            });
          }
        });
      }
      memoryEvents = list;
      emitAggregated();
    });

    // 2. Real ride_requests from Android passenger app
    const reqRef = ref(db, 'ride_requests');
    const unsubReq = onValue(reqRef, (snap) => {
      const val = snap.val();
      const list: LiveActivityFeedItem[] = [];
      if (val && typeof val === 'object') {
        Object.entries(val).forEach(([key, r]: [string, any]) => {
          if (r && typeof r === 'object') {
            const statusUpper = String(r.status || 'REQUESTED').toUpperCase();
            const timeStr = r.timestamp
              ? new Date(r.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
              : 'Live';
            if (statusUpper === 'CANCELLED') {
              list.push({
                id: `mut-req-canc-${key}`,
                type: 'trip_cancelled',
                title: `Ride Cancelled (${r.rideCategory || r.vehicleType || 'Car'})`,
                description: `${r.passengerName || 'Passenger'} cancelled trip request for ${r.pickupTitle || r.pickupAddress || 'Peshawar'}.`,
                severity: 'warning',
                timestamp: timeStr,
                tripId: key
              });
            } else if (statusUpper === 'COMPLETED') {
              list.push({
                id: `mut-req-comp-${key}`,
                type: 'trip_completed',
                title: `Ride Completed (${r.rideCategory || r.vehicleType || 'Car'})`,
                description: `${r.passengerName || 'Passenger'} trip completed. Agreed Fare: Rs. ${r.estimatedFare || r.fare || 0}.`,
                severity: 'success',
                timestamp: timeStr,
                tripId: key
              });
            } else {
              list.push({
                id: `mut-req-${key}`,
                type: 'trip_request',
                title: `Ride Request: ${r.passengerName || 'Passenger'}`,
                description: `${r.pickupTitle || 'Pickup'} → ${r.destinationTitle || 'Destination'} (Est. Rs. ${r.estimatedFare || 0}).`,
                severity: 'info',
                timestamp: timeStr,
                tripId: key
              });
            }
          }
        });
      }
      requestEvents = list;
      emitAggregated();
    });

    // 3. Real safety reports from Android apps
    const safeRef = ref(db, 'safety_reports_admin');
    const unsubSafe = onValue(safeRef, (snap) => {
      const val = snap.val();
      const list: LiveActivityFeedItem[] = [];
      if (val && typeof val === 'object') {
        Object.entries(val).forEach(([key, r]: [string, any]) => {
          if (r && typeof r === 'object') {
            const timeStr = r.timestamp
              ? new Date(r.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
              : 'Recent';
            list.push({
              id: `mut-safety-${key}`,
              type: 'sos_alert',
              title: `Safety Incident: ${r.categoryLabel || r.category || 'Alert'}`,
              description: `Reported by ${r.reporterName || 'Passenger'} on ride ${String(r.rideId || '').slice(0, 8)}... (${r.ridePickupTitle || 'Pickup'} → ${r.rideDestinationTitle || 'Dropoff'}). Status: ${r.status}`,
              severity: 'critical',
              timestamp: timeStr,
              tripId: r.rideId
            });
          }
        });
      }
      safetyEvents = list;
      emitAggregated();
    });

    // 4. Real driver verifications & status
    const drvRef = ref(db, 'driver_verifications');
    const unsubDrv = onValue(drvRef, (snap) => {
      const val = snap.val();
      const list: LiveActivityFeedItem[] = [];
      if (val && typeof val === 'object') {
        Object.entries(val).forEach(([key, r]: [string, any]) => {
          if (r && typeof r === 'object') {
            const statusUpper = String(r.status || r.verificationStatus || '').toUpperCase();
            list.push({
              id: `mut-drv-${key}`,
              type: 'driver_kyc',
              title: `Driver Verification: ${r.driverName || r.fullName || 'Fleet Captain'}`,
              description: `${r.vehicleMake || 'Toyota'} ${r.vehicleModel || 'Corolla'} (${r.licensePlate || 'LED-5031'}). Verification status: ${statusUpper || 'PENDING'}.`,
              severity: statusUpper === 'APPROVED' ? 'success' : 'warning',
              timestamp: 'Live',
              driverId: r.driverId || key
            });
          }
        });
      }
      driverEvents = list;
      emitAggregated();
    });

    // 5. Real payout requests
    const payRef = ref(db, 'payout_requests');
    const unsubPay = onValue(payRef, (snap) => {
      const val = snap.val();
      const list: LiveActivityFeedItem[] = [];
      if (val && typeof val === 'object') {
        Object.entries(val).forEach(([key, r]: [string, any]) => {
          if (r && typeof r === 'object') {
            const isProcessed = r.status === 'processed';
            list.push({
              id: `mut-pay-${key}`,
              type: 'payout_requested',
              title: `Driver Settlement #${key.slice(0, 7)}: ${r.status?.toUpperCase() || 'PENDING'}`,
              description: `Rs. ${r.amount || 0} via ${(r.paymentMethod || 'Wallet').toUpperCase()}. ${r.transactionRef ? `Ref: ${r.transactionRef}` : ''}`,
              severity: isProcessed ? 'success' : 'warning',
              timestamp: r.requestedAt || 'Recent',
              driverId: r.driverId
            });
          }
        });
      }
      payoutEvents = list;
      emitAggregated();
    });

    return () => {
      unsubMem();
      unsubReq();
      unsubSafe();
      unsubDrv();
      unsubPay();
    };
  }

  // Fallback for Firestore
  if (firestore) {
    const colRef = collection(firestore, 'system_memory');
    const unsub = onSnapshot(
      colRef,
      (snapshot) => {
        const list: LiveActivityFeedItem[] = [];
        snapshot.forEach((docSnap) => {
          list.push({ id: docSnap.id, ...docSnap.data() } as LiveActivityFeedItem);
        });
        callback(list);
      },
      (err) => {
        console.warn('Firestore system_memory listener note:', err);
      }
    );
    return () => unsub();
  }

  return () => {};
}





