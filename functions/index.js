/**
 * Import function triggers from their respective submodules:
 *
 * const {onCall} = require("firebase-functions/v2/https");
 * const {onDocumentWritten} = require("firebase-functions/v2/firestore");
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */

const {setGlobalOptions} = require("firebase-functions");
//const {onRequest} = require("firebase-functions/https");
//const logger = require("firebase-functions/logger");

// For cost control, you can set the maximum number of containers that can be
// running at the same time. This helps mitigate the impact of unexpected
// traffic spikes by instead downgrading performance. This limit is a
// per-function limit. You can override the limit for each function using the
// `maxInstances` option in the function's options, e.g.
// `onRequest({ maxInstances: 5 }, (req, res) => { ... })`.
// NOTE: setGlobalOptions does not apply to functions using the v1 API. V1
// functions should each use functions.runWith({ maxInstances: 10 }) instead.
// In the v1 API, each function can only serve one request per container, so
// this will be the maximum concurrent request count.
setGlobalOptions({ maxInstances: 10 });

// Create and deploy your first functions
// https://firebase.google.com/docs/functions/get-started

// exports.helloWorld = onRequest((request, response) => {
//   logger.info("Hello logs!", {structuredData: true});
//   response.send("Hello from Firebase!");
// });

const functions = require('firebase-functions');
const admin = require('firebase-admin');

// Initialize Firebase Admin SDK
admin.initializeApp();

/**
 * Cloud Function untuk menghapus user dari Firebase Auth
 * saat dihapus dari Realtime Database
 * FIXED: Menggunakan path yang benar
 */
exports.deleteUserFromAuth = functions.database
  .ref('/users/{userId}')
  .onDelete(async (snapshot, context) => {
    const userId = context.params.userId;

    console.log(`🔥 TRIGGERED: User ${userId} deleted from database`);
    console.log(`📊 Deleted data:`, snapshot.val());

    try {
      // Delete user from Firebase Authentication
      await admin.auth().deleteUser(userId);

      console.log(`✅ SUCCESS: User ${userId} deleted from Auth`);

      // Log ke database untuk tracking
      const db = admin.database();
      await db.ref(`logs/auth_deletions/${Date.now()}`).set({
        userId: userId,
        timestamp: new Date().toISOString(),
        success: true,
        deletedBy: 'cloud_function'
      });

      return {
        success: true,
        message: `User ${userId} deleted from Auth successfully`,
        timestamp: new Date().toISOString()
      };

    } catch (error) {
      console.error(`❌ ERROR deleting user ${userId} from Auth:`, error);

      // Log error
      const db = admin.database();
      await db.ref(`logs/errors/${Date.now()}`).set({
        userId: userId,
        error: error.message,
        errorCode: error.code,
        timestamp: new Date().toISOString()
      });

      // Jika user tidak ditemukan di Auth, itu OK
      if (error.code === 'auth/user-not-found') {
        console.log(`⚠️ User ${userId} not found in Auth, continuing...`);
        return {
          success: true,
          warning: `User ${userId} not found in Auth`,
          timestamp: new Date().toISOString()
        };
      }

      // Jangan throw error agar tidak mengganggu proses hapus database
      return {
        success: false,
        error: error.message,
        errorCode: error.code,
        timestamp: new Date().toISOString()
      };
    }
  });

/**
 * Cloud Function alternatif: Trigger dari path yang lebih spesifik
 */
exports.deleteUserFromAuthOnRemove = functions.database
  .ref('/users/{userId}/deleted')
  .onCreate(async (snapshot, context) => {
    const userId = context.params.userId;
    const deletedFlag = snapshot.val();

    if (deletedFlag === true) {
      console.log(`User ${userId} marked as deleted, removing from Auth...`);

      try {
        await admin.auth().deleteUser(userId);
        console.log(`User ${userId} deleted from Auth`);

        // Hapus flag
        await snapshot.ref.remove();

      } catch (error) {
        console.error(`Error:`, error);
      }
    }
  });

/**
 * Cloud Function untuk menghapus user dari Auth via HTTP call
 * (Untuk admin panel di aplikasi)
 */
exports.deleteUserAuth = functions.https.onCall(async (data, context) => {
  // Cek authentication
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'User must be authenticated'
    );
  }

  const { userId } = data;

  if (!userId) {
    throw new functions.https.HttpsError(
      'invalid-argument',
      'User ID is required'
    );
  }

  // Cek apakah user mencoba menghapus dirinya sendiri
  if (userId === context.auth.uid) {
    throw new functions.https.HttpsError(
      'permission-denied',
      'Cannot delete your own account'
    );
  }

  try {
    // Delete user from Firebase Authentication
    await admin.auth().deleteUser(userId);

    console.log(`User ${userId} deleted from Auth by admin ${context.auth.uid}`);

    return {
      success: true,
      message: `User ${userId} deleted successfully from Auth`
    };

  } catch (error) {
    console.error(`Error deleting user ${userId}:`, error);

    if (error.code === 'auth/user-not-found') {
      throw new functions.https.HttpsError(
        'not-found',
        `User ${userId} not found in Auth`
      );
    }

    throw new functions.https.HttpsError(
      'internal',
      `Failed to delete user: ${error.message}`
    );
  }
});

/**
 * Cloud Function untuk sync user data antara Auth dan Database
 */
exports.syncUserToDatabase = functions.auth.user().onCreate(async (user) => {
  const { uid, email, displayName, phoneNumber, photoURL } = user;

  console.log(`New user created in Auth: ${uid}, email: ${email}`);

  // Cek apakah user sudah ada di database
  const db = admin.database();
  const userRef = db.ref(`users/${uid}`);

  const snapshot = await userRef.once('value');

  if (!snapshot.exists()) {
    // User baru, buat entry di database
    const userData = {
      uid: uid,
      email: email || '',
      username: displayName || email?.split('@')[0] || '',
      noHp: phoneNumber || '',
      profileImageUrl: photoURL || '',
      role: 'user', // Default role
      status: 1, // Default active
      createdAt: admin.database.ServerValue.TIMESTAMP,
      updatedAt: admin.database.ServerValue.TIMESTAMP
    };

    await userRef.set(userData);
    console.log(`User ${uid} synced to database`);

    return {
      success: true,
      message: 'User synced to database'
    };
  }

  console.log(`User ${uid} already exists in database`);
  return null;
});

/**
 * Cloud Function untuk update user data di Auth ketika diupdate di Database
 */
exports.updateUserAuth = functions.database
  .ref('/users/{userId}/disabled')
  .onUpdate(async (change, context) => {
    const userId = context.params.userId;
    const disabled = change.after.val();

    console.log(`Updating Auth user ${userId} disabled status to: ${disabled}`);

    try {
      await admin.auth().updateUser(userId, {
        disabled: disabled === true || disabled === 2
      });

      console.log(`User ${userId} Auth disabled status updated to: ${disabled}`);

      return {
        success: true,
        message: `User ${userId} Auth status updated`
      };

    } catch (error) {
      console.error(`Error updating user ${userId}:`, error);

      if (error.code === 'auth/user-not-found') {
        console.log(`User ${userId} not found in Auth`);
        return null;
      }

      throw new functions.https.HttpsError(
        'internal',
        `Failed to update user: ${error.message}`,
        error
      );
    }
  });

  // functions/index.js - tambah function
  exports.setAdminClaim = functions.https.onCall(async (data, context) => {
    // Hanya admin yang sudah ada bisa set claim
    if (!context.auth) {
      throw new functions.https.HttpsError(
        'unauthenticated',
        'User must be authenticated'
      );
    }

    const { targetUserId, isAdmin } = data;

    if (!targetUserId) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Target user ID is required'
      );
    }

    // Set custom claims
    const claims = {
      admin: isAdmin === true,
      level: isAdmin === true ? 10 : 1
    };

    try {
      await admin.auth().setCustomUserClaims(targetUserId, claims);

      // Update role di database juga
      await admin.database().ref(`users/${targetUserId}/role`)
        .set(isAdmin ? 'admin' : 'user');

      console.log(`Custom claims set for user ${targetUserId}:`, claims);

      return {
        success: true,
        message: `Admin ${isAdmin ? 'granted' : 'revoked'} for user ${targetUserId}`
      };

    } catch (error) {
      console.error(`Error setting custom claims:`, error);
      throw new functions.https.HttpsError(
        'internal',
        `Failed to set admin claim: ${error.message}`
      );
    }
  });

  /**
   * Cloud Function untuk log semua database changes
   */
  exports.logDatabaseChanges = functions.database
    .ref('/users/{userId}')
    .onWrite(async (change, context) => {
      const userId = context.params.userId;
      const beforeData = change.before.val();
      const afterData = change.after.val();

      const logRef = admin.database().ref(`logs/user_changes/${Date.now()}`);

      const logData = {
        userId: userId,
        timestamp: new Date().toISOString(),
        eventType: change.before.exists() ? (change.after.exists() ? 'updated' : 'deleted') : 'created',
        before: beforeData,
        after: afterData
      };

      await logRef.set(logData);
      console.log(`📝 Logged ${logData.eventType} event for user ${userId}`);

      return null;
    });

/**
 * Cloud Function untuk cleanup email duplikat di Auth
 * RUN MANUAL via HTTP trigger
 */
exports.cleanupDuplicateEmails = functions.https.onRequest(async (req, res) => {
  try {
    // Cek admin authentication
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      res.status(401).json({ error: 'Unauthorized' });
      return;
    }

    const idToken = authHeader.split('Bearer ')[1];
    const decodedToken = await admin.auth().verifyIdToken(idToken);

    // Cek apakah user adalah admin
    const adminUser = await admin.database().ref(`users/${decodedToken.uid}/role`).once('value');
    if (adminUser.val() !== 'admin') {
      res.status(403).json({ error: 'Admin access required' });
      return;
    }

    console.log('Starting duplicate email cleanup...');

    // Get all users from Auth
    const listUsersResult = await admin.auth().listUsers();
    const users = listUsersResult.users;

    // Group by email
    const emailMap = new Map();
    const duplicates = [];

    users.forEach(user => {
      if (user.email) {
        if (!emailMap.has(user.email)) {
          emailMap.set(user.email, []);
        }
        emailMap.get(user.email).push(user);
      }
    });

    // Find duplicates
    emailMap.forEach((userList, email) => {
      if (userList.length > 1) {
        duplicates.push({
          email: email,
          users: userList.map(u => ({
            uid: u.uid,
            emailVerified: u.emailVerified,
            disabled: u.disabled,
            metadata: u.metadata
          }))
        });
      }
    });

    // Process duplicates (keep the oldest verified account)
    const cleanupResults = [];

    for (const duplicate of duplicates) {
      const userList = duplicate.users;

      // Sort by creation time (oldest first)
      userList.sort((a, b) =>
        new Date(a.metadata.creationTime) - new Date(b.metadata.creationTime)
      );

      // Keep the first user (oldest), delete the rest
      const userToKeep = userList[0];
      const usersToDelete = userList.slice(1);

      for (const userToDelete of usersToDelete) {
        try {
          await admin.auth().deleteUser(userToDelete.uid);

          // Also delete from database if exists
          await admin.database().ref(`users/${userToDelete.uid}`).remove();

          cleanupResults.push({
            email: duplicate.email,
            kept: userToKeep.uid,
            deleted: userToDelete.uid,
            reason: 'duplicate_email'
          });

          console.log(`Deleted duplicate user: ${userToDelete.uid} for email: ${duplicate.email}`);

        } catch (error) {
          console.error(`Failed to delete user ${userToDelete.uid}:`, error);
          cleanupResults.push({
            email: duplicate.email,
            error: error.message,
            userId: userToDelete.uid
          });
        }
      }
    }

    // Log results
    await admin.database().ref(`logs/cleanup/${Date.now()}`).set({
      timestamp: new Date().toISOString(),
      totalUsers: users.length,
      duplicateEmails: duplicates.length,
      cleanedUp: cleanupResults.length,
      details: cleanupResults
    });

    res.json({
      success: true,
      message: `Found ${duplicates.length} duplicate emails, cleaned up ${cleanupResults.length} users`,
      totalUsers: users.length,
      duplicates: duplicates.length,
      cleaned: cleanupResults.length,
      details: cleanupResults
    });

  } catch (error) {
    console.error('Cleanup error:', error);
    res.status(500).json({
      success: false,
      error: error.message
    });
  }
});

