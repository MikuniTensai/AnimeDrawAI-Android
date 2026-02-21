# Enhanced Security for Update Profile Image

## Current Security (Already Implemented) ✅

1. **Firebase Authentication** - Token verification
2. **Ownership Check** - User can only update their own character
3. **Input Validation** - Required parameters check
4. **Character Existence** - Verify character exists

## Recommended Enhancements

### 1. URL Validation & Whitelisting

```python
def update_character_profile_image(self):
    """Update character profile image from a photo URL."""
    try:
        from flask import g
        from urllib.parse import urlparse
        
        user_id = getattr(g, 'user_id', None)
        if not user_id:
            return jsonify({"success": False, "error": "Unauthorized"}), 401
        
        data = request.get_json()
        char_id = data.get('characterId')
        image_url = data.get('imageUrl')
        
        if not char_id or not image_url:
            return jsonify({"success": False, "error": "Missing characterId or imageUrl"}), 400
        
        # SECURITY: Validate URL format
        if not image_url.startswith(('http://', 'https://')):
            return jsonify({"success": False, "error": "Invalid URL format"}), 400
        
        # SECURITY: Whitelist allowed domains
        allowed_domains = [
            'your-server-domain.com',  # Your API server
            'firebasestorage.googleapis.com',  # Firebase Storage
            'storage.googleapis.com',  # Google Cloud Storage
            'localhost',  # For development
            '127.0.0.1'   # For development
        ]
        
        parsed = urlparse(image_url)
        domain_allowed = any(
            parsed.netloc == domain or parsed.netloc.endswith('.' + domain)
            for domain in allowed_domains
        )
        
        if not domain_allowed:
            self.logger.warning(f"Blocked profile update from untrusted domain: {parsed.netloc}")
            return jsonify({
                "success": False, 
                "error": "Image must be from allowed sources"
            }), 400
        
        # SECURITY: Check URL length (prevent DOS)
        if len(image_url) > 2048:
            return jsonify({"success": False, "error": "URL too long"}), 400
        
        char_ref = self.config.db.collection('characters').document(char_id)
        char_doc = char_ref.get()
        
        if not char_doc.exists:
            return jsonify({"success": False, "error": "Character not found"}), 404
        
        char_data = char_doc.to_dict()
        
        # Verify ownership
        if char_data.get('userId') and char_data.get('userId') != user_id:
            return jsonify({"success": False, "error": "Not your character"}), 403
        
        # SECURITY: Rate limit check (prevent spam)
        last_update = char_data.get('profileUpdatedAt')
        if last_update:
            from datetime import datetime, timedelta
            try:
                last_dt = datetime.fromisoformat(last_update)
                if datetime.now() - last_dt < timedelta(minutes=5):
                    return jsonify({
                        "success": False,
                        "error": "Please wait 5 minutes between profile updates"
                    }), 429
            except:
                pass  # If parsing fails, allow update
        
        # Update profile image
        char_ref.update({
            'imageUrl': image_url,
            'profileUpdatedAt': datetime.now().isoformat()
        })
        
        return jsonify({
            "success": True,
            "message": "Profile image updated successfully",
            "imageUrl": image_url
        })
        
    except Exception as e:
        self.logger.error(f"Error updating profile image: {e}")
        return jsonify({"success": False, "error": str(e)}), 500
```

### 2. Add Rate Limiting to Route

In `_setup_routes()`:

```python
# Add rate limit: 10 updates per hour per user
self.app.route('/character/update-profile-image', methods=['POST'])(
    self.limiter.limit("10 per hour")(self.update_character_profile_image)
)
```

### 3. Firestore Security Rules

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /characters/{characterId} {
      // Allow read for authenticated users
      allow read: if request.auth != null;
      
      // Allow create only for authenticated users
      allow create: if request.auth != null
        && request.auth.uid == request.resource.data.userId;
      
      // Allow update only for owner and only specific fields
      allow update: if request.auth != null 
        && request.auth.uid == resource.data.userId
        && request.resource.data.diff(resource.data).affectedKeys()
           .hasOnly(['imageUrl', 'profileUpdatedAt', 'notificationEnabled', 'notificationUnlocked']);
      
      // Allow delete only for owner
      allow delete: if request.auth != null
        && request.auth.uid == resource.data.userId;
    }
  }
}
```

### 4. Content Moderation (Optional)

For production, consider adding image content moderation:

```python
# Optional: Use Google Cloud Vision API to check image content
from google.cloud import vision

def is_safe_image(image_url):
    """Check if image is safe using Cloud Vision API."""
    try:
        client = vision.ImageAnnotatorClient()
        image = vision.Image()
        image.source.image_uri = image_url
        
        response = client.safe_search_detection(image=image)
        safe = response.safe_search_annotation
        
        # Block if adult, violence, or racy content detected
        if (safe.adult >= 3 or safe.violence >= 3 or safe.racy >= 3):
            return False
        return True
    except Exception as e:
        # If check fails, allow (don't block legitimate users)
        return True

# In update_character_profile_image:
if not is_safe_image(image_url):
    return jsonify({
        "success": False,
        "error": "Image content not appropriate"
    }), 400
```

## Security Checklist

- [x] Firebase Authentication required
- [x] Ownership verification
- [x] Input validation
- [ ] URL format validation (recommended)
- [ ] Domain whitelisting (recommended)
- [ ] Rate limiting per user (recommended)
- [ ] Firestore security rules (critical)
- [ ] Content moderation (optional, for production)

## Conclusion

**Current implementation is SAFE for basic use**, but adding the recommended enhancements will make it production-ready and protect against:
- Malicious URL injection
- Spam/abuse
- Unauthorized access
- NSFW content

Priority:
1. **HIGH**: Firestore security rules
2. **MEDIUM**: URL validation & whitelisting
3. **MEDIUM**: Rate limiting
4. **LOW**: Content moderation (only if needed)
