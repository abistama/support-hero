# SupportHero 🦸‍♀️

> From Slack to Jira, support internal users where they are!

Tired of "can you create me a ticket?" when people ask a question on Slack? **SupportHero** is your support companion that removes friction for users who request support and keeps the processes for the teams that provide support.

## ✨ Features

### 🎯 **Reaction-Based Ticket Creation**
- Configure emoji reactions (like 🎫 `:ticket:` or 🔥 `:fire:`) that automatically create Jira tickets
- Support for user-specific and user group-specific configurations
- Channel-specific or workspace-wide reaction setups

### 🤖 **AI-Powered Smart Titles**
- Optional AI summarization for intelligent ticket titles
- Converts long Slack messages into concise, meaningful Jira ticket summaries
- Graceful fallback to text truncation if AI fails

### 📊 **Customer Satisfaction (CSAT) Tracking**
- Automatic CSAT surveys sent to ticket reporters
- Configurable reminder system for pending surveys
- Analytics and reporting on support quality

### 🔗 **Seamless Integration**
- **Slack**: Native app with shortcuts, interactive messages, and reaction handling
- **Jira**: Full OAuth integration with automatic token refresh
- **Multi-tenant**: Support for multiple Slack workspaces and Jira instances

### ⚙️ **Advanced Configuration**
- Per-reaction configuration with project mapping
- Custom feedback messages with variable substitution
- Label and component assignment
- Issue type customization

## 🚀 Use Cases

### 1. **IT Support Team**
```
👤 User: "Hey, I can't access the VPN from home. It keeps timing out."
🎫 Someone reacts with :ticket:
🤖 SupportHero creates Jira ticket: "VPN connection timeout issue from home office"
💬 Automated response: "Hi @user! We've created ticket IT-123 for your VPN issue."
📊 CSAT survey sent after ticket resolution
```

### 2. **HR Department**
```
👤 Employee: "I need help updating my emergency contact information in the system."
🔥 HR team member reacts with :fire: (urgent HR requests)
🤖 Creates ticket: "Emergency contact information update request"
📋 Automatically assigned to HR project with "Personal Info" component
```

### 3. **DevOps Incidents**
```
👤 Developer: "Production API is returning 500 errors for all /users endpoints since 2PM"
🚨 DevOps reacts with :rotating_light:
🤖 Creates P1 incident: "Production API 500 errors on /users endpoints"
⚡ Instantly routed to DevOps project with "Critical" priority
```

### 4. **Security Requests**
```
👤 Manager: "I need access to the finance dashboard for Q4 reporting"
🔐 Security team reacts with :lock:
🤖 Creates access request: "Finance dashboard access for Q4 reporting"
🔍 Auto-tagged with "access-request" and "finance" labels
```

## 🏗 Architecture

### System Overview

Available infrastructure [diagram](https://excalidraw.com/#json=cJXAVECA_NGp5DqbuPwTe,klNRLnymGBPGKtYUc7dhI).

### Key Components

#### **Application Layer**
- **Slack Integration**: Event handling, OAuth, message processing
- **Jira Integration**: Ticket creation, project management, OAuth token refresh
- **AI Summarization**: LangChain4j integration for intelligent title generation
- **CSAT Management**: Survey delivery and reminder scheduling

#### **Domain Layer**
- **Configuration Management**: Reaction mappings, user permissions, project settings
- **Event Processing**: Reaction events, ticket lifecycle, user interactions
- **Multi-tenancy**: Isolated configurations per Slack workspace

#### **Infrastructure Layer**
- **Database**: PostgreSQL with JOOQ for type-safe queries
- **Caching**: Redis for event deduplication and temporary data
- **Security**: Per-tenant encryption for OAuth tokens
- **External APIs**: Slack SDK, Jira REST API, AI services

## 🎮 Usage

### Setup Process

1. **Install the Slack App**
   ```bash
   # Configure your Slack app using the manifest
   cp slack-app-manifest.json your-app-manifest.json
   # Update URLs in the manifest for your deployment
   ```

2. **Connect to Jira**
   - Use the global shortcut "Configure new reaction"
   - Authenticate with your Jira instance
   - SupportHero handles OAuth flow automatically

3. **Configure Reactions**
   ```
   /shortcut "Configure new reaction"
   
   📋 Select Jira Project: "IT Support"
   🎯 Choose Reaction: 🎫 :ticket:
   👥 Assign to User/Group: @it-support-team
   📝 Issue Type: "Task"
   💬 Feedback Message: "Hi $reporter! Created ticket $issue for you."
   🤖 AI Summarizer: ✅ Enabled
   📊 Send CSAT: ✅ Enabled
   ```

4. **Start Using**
   - React to any Slack message with your configured emoji
   - SupportHero automatically creates Jira tickets
   - Users get instant feedback with ticket links

### Configuration Options

#### **Reaction Configuration**
- **Project Mapping**: Link reactions to specific Jira projects
- **User/Group Permissions**: Control who can create tickets
- **Channel Scope**: Global or channel-specific reactions
- **AI Summarization**: Enable intelligent title generation
- **CSAT Surveys**: Automatic customer satisfaction tracking

#### **Feedback Messages**
Support dynamic variables in feedback messages:
- `$reporter` - @mention of the ticket reporter
- `$issue` - Jira ticket key (e.g., IT-123)

Example:
```
"Thanks $reporter! 🎫 Created $issue for your request. We'll get back to you soon!"
```

## 🔧 Development

### Prerequisites
- **Java 17+**
- **Kotlin 1.9+** 
- **PostgreSQL 16+**
- **Redis 7+**
- **Slack App** with appropriate permissions
- **Jira Cloud** instance with API access

### Local Setup

1. **Database Setup**
   ```bash
   # Start dependencies
   ./deps start
   
   # Run migrations
   ./gradlew liquibaseUpdate
   ```

2. **Environment Configuration**
   ```bash
   # Copy and configure environment
   cp .env.properties.example .env.properties
   
   # Required variables:
   ENCRYPTION_MASTER_KEY=your-secure-master-key
   SLACK_CLIENT_SECRET=your-slack-client-secret
   SLACK_SIGNING_SECRET=your-slack-signing-secret
   SLACK_REDIRECT_HOST=https://your-ngrok-url.ngrok.io
   JIRA_CLIENT_SECRET=your-jira-oauth-secret
   ANTHROPIC_API_KEY=your-ai-api-key
   ```

3. **Slack App Configuration**
   ```bash
   # Generate environment-specific manifest
   ./configure-slack-app dev
   
   # Use the generated manifest in Slack App settings
   cat slack-app-dev-manifest.json
   ```

4. **Run Application**
   ```bash
   ./gradlew bootRun
   ```

### Testing
```bash
# Run all tests
./gradlew test

# Run specific test suites
./gradlew test --tests "*CreateJiraFromReactionTest*"
./gradlew test --tests "*TenantEncryptionServiceTest*"
```

### Security Features

#### **Per-Tenant Token Encryption**
- OAuth tokens encrypted with tenant-specific keys
- AES-256-GCM encryption with PBKDF2 key derivation
- Cross-tenant isolation prevents data leaks

See [encryption setup](docs/encryption_setup.md) for detailed security configuration.

## 📈 Monitoring and Analytics

### Built-in Metrics
- **Ticket Creation Rate**: Reactions → Jira tickets per period
- **CSAT Scores**: Average satisfaction ratings per configuration
- **Response Times**: Time from reaction to ticket resolution
- **User Adoption**: Active users and reaction usage patterns

## 🤝 Contributing

### Development Workflow
1. **Clone and Setup**
   ```bash
   git clone https://github.com/abistama/supporthero.git
   cd supporthero
   ./deps start
   ```

2. **Make Changes**
   - Follow Kotlin coding conventions
   - Add tests for new features
   - Update documentation as needed

3. **Test and Deploy**
   ```bash
   ./gradlew test
   ./gradlew build
   ```

### Architecture Decisions
- **Kotlin + Spring Boot**: Type safety, coroutines, reactive programming
- **JOOQ**: Type-safe SQL with compile-time verification
- **Arrow**: Functional programming for error handling
- **LangChain4j**: AI integration with multiple provider support

## 🆘 Support

- **Issues**: Create GitHub issues for bugs and feature requests
- **Discussions**: Use GitHub Discussions for questions and ideas
- **Contact**: Reach out via email at contact at abistama dot com.

---

**Made with ❤️ for support teams everywhere**

*SupportHero - Because great support starts where your users are*